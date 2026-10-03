package com.hits100m.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import java.net.URL;

public class MediaPlaybackService extends Service {
    public static final String CHANNEL_ID = "hits8d_playback_channel";
    public static final int NOTIFICATION_ID = 1001;

    public static final String ACTION_TOGGLE = "com.hits100m.app.ACTION_TOGGLE";
    public static final String ACTION_PREV = "com.hits100m.app.ACTION_PREV";
    public static final String ACTION_NEXT = "com.hits100m.app.ACTION_NEXT";
    public static final String ACTION_REWIND_10 = "com.hits100m.app.ACTION_REWIND_10";
    public static final String ACTION_FORWARD_10 = "com.hits100m.app.ACTION_FORWARD_10";
    public static final String ACTION_SEEK_TO = "com.hits100m.app.ACTION_SEEK_TO";
    public static final String ACTION_UPDATE_PROGRESS = "com.hits100m.app.ACTION_UPDATE_PROGRESS";
    public static final String ACTION_STOP = "com.hits100m.app.ACTION_STOP";
    public static final String ACTION_UPDATE_TRACK = "com.hits100m.app.ACTION_UPDATE_TRACK";
    public static final String ACTION_UPDATE_STATE = "com.hits100m.app.ACTION_UPDATE_STATE";

    public interface RemoteControlListener {
        void onRemoteToggle();
        void onRemoteNext();
        void onRemotePrev();
        void onRemoteRewind10();
        void onRemoteForward10();
        void onRemoteSeekTo(long posMs);
        void onRemoteStop();
        void onHeadsetDisconnected();
    }

    private static RemoteControlListener listener;
    private static volatile MediaPlaybackService instance;

    private MediaSession mediaSession;
    private PowerManager.WakeLock wakeLock;
    private BroadcastReceiver disconnectReceiver;

    private String currentTitle = "8D Audio";
    private String currentArtist = "Use Headphones";
    private String currentViews = "";
    private String currentThumbUrl = "";
    private Bitmap currentBitmap = null;
    private boolean isPlaying = false;
    private long currentPositionMs = 0;
    private long currentDurationMs = 0;

    public static void setListener(RemoteControlListener l) {
        listener = l;
    }

    public static MediaPlaybackService getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        createNotificationChannel();
        initMediaSession();
        initWakeLock();
        registerDisconnectReceivers();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "8D Audio Playback",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Background 8D audio playback controls");
            channel.setShowBadge(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    private void initMediaSession() {
        try {
            mediaSession = new MediaSession(this, "8DAudio_Session");
            mediaSession.setCallback(new MediaSession.Callback() {
                @Override
                public void onPlay() {
                    handleToggle();
                }

                @Override
                public void onPause() {
                    handleToggle();
                }

                @Override
                public void onSkipToNext() {
                    handleNext();
                }

                @Override
                public void onSkipToPrevious() {
                    handlePrev();
                }

                @Override
                public void onRewind() {
                    handleRewind10();
                }

                @Override
                public void onFastForward() {
                    handleForward10();
                }

                @Override
                public void onSeekTo(long pos) {
                    handleSeekTo(pos);
                }

                @Override
                public void onStop() {
                    handleStop();
                }
            });

            mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
            mediaSession.setActive(true);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void initWakeLock() {
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "8DAudio:WakeLock");
                wakeLock.setReferenceCounted(false);
            }
        } catch (Throwable ignored) {}
    }

    private void registerDisconnectReceivers() {
        try {
            disconnectReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent == null) return;
                    String action = intent.getAction();
                    if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(action) ||
                        BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                        handleHeadsetDisconnected();
                    } else if (Intent.ACTION_HEADSET_PLUG.equals(action)) {
                        int state = intent.getIntExtra("state", -1);
                        if (state == 0) { // Wired headset unplugged
                            handleHeadsetDisconnected();
                        }
                    } else if (BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED.equals(action) ||
                               BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
                        int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);
                        if (state == BluetoothProfile.STATE_DISCONNECTED) {
                            handleHeadsetDisconnected();
                        }
                    }
                }
            };

            IntentFilter filter = new IntentFilter();
            filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
            filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
            filter.addAction(Intent.ACTION_HEADSET_PLUG);
            filter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
            filter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
            registerReceiver(disconnectReceiver, filter);
        } catch (Throwable ignored) {}
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            if (ACTION_TOGGLE.equals(action)) {
                handleToggle();
            } else if (ACTION_NEXT.equals(action)) {
                handleNext();
            } else if (ACTION_PREV.equals(action)) {
                handlePrev();
            } else if (ACTION_REWIND_10.equals(action)) {
                handleRewind10();
            } else if (ACTION_FORWARD_10.equals(action)) {
                handleForward10();
            } else if (ACTION_SEEK_TO.equals(action)) {
                long pos = intent.getLongExtra("position", currentPositionMs);
                handleSeekTo(pos);
            } else if (ACTION_STOP.equals(action)) {
                handleStop();
            } else if (ACTION_UPDATE_TRACK.equals(action)) {
                currentTitle = intent.getStringExtra("title");
                currentArtist = intent.getStringExtra("artist");
                currentViews = intent.getStringExtra("views");
                currentThumbUrl = intent.getStringExtra("thumbUrl");
                isPlaying = intent.getBooleanExtra("isPlaying", true);
                currentPositionMs = 0;
                currentDurationMs = 0;
                if (currentTitle == null) currentTitle = "8D Audio";
                if (currentArtist == null) currentArtist = "";
                if (currentViews == null) currentViews = "";

                // Immediate notification update with 0ms delay
                postForegroundNotification();
                updateSystemMetadata();

                // Asynchronous artwork loader
                loadArtworkAsync(currentThumbUrl);
            } else if (ACTION_UPDATE_STATE.equals(action)) {
                isPlaying = intent.getBooleanExtra("isPlaying", false);
                updateSystemPlaybackState();
                postForegroundNotification();
            } else if (ACTION_UPDATE_PROGRESS.equals(action)) {
                long pos = intent.getLongExtra("position", currentPositionMs);
                long dur = intent.getLongExtra("duration", currentDurationMs);
                boolean playing = intent.getBooleanExtra("isPlaying", isPlaying);
                updateProgressDirect(pos, dur, playing);
            }
        }
        return START_NOT_STICKY;
    }

    // Direct real-time progress update
    public void updateProgressDirect(long posMs, long durMs, boolean playing) {
        this.currentPositionMs = posMs;
        this.isPlaying = playing;
        boolean durationChanged = false;
        if (durMs > 0 && durMs != this.currentDurationMs) {
            this.currentDurationMs = durMs;
            durationChanged = true;
        }

        updateSystemPlaybackState();
        if (durationChanged) {
            updateSystemMetadata();
        }
    }

    private void handleToggle() {
        if (listener != null) {
            listener.onRemoteToggle();
        }
    }

    private void handleNext() {
        if (listener != null) {
            listener.onRemoteNext();
        }
    }

    private void handlePrev() {
        if (listener != null) {
            listener.onRemotePrev();
        }
    }

    private void handleRewind10() {
        currentPositionMs = Math.max(0, currentPositionMs - 10000);
        updateSystemPlaybackState();
        if (listener != null) {
            listener.onRemoteRewind10();
        }
    }

    private void handleForward10() {
        if (currentDurationMs > 0) {
            currentPositionMs = Math.min(currentDurationMs, currentPositionMs + 10000);
        } else {
            currentPositionMs += 10000;
        }
        updateSystemPlaybackState();
        if (listener != null) {
            listener.onRemoteForward10();
        }
    }

    private void handleSeekTo(long pos) {
        currentPositionMs = pos;
        updateSystemPlaybackState();
        if (listener != null) {
            listener.onRemoteSeekTo(pos);
        }
    }

    private void handleStop() {
        isPlaying = false;
        if (listener != null) {
            listener.onRemoteStop();
        }

        try {
            stopForeground(true);
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.cancel(NOTIFICATION_ID);
            }
        } catch (Throwable ignored) {}

        if (wakeLock != null && wakeLock.isHeld()) {
            try { wakeLock.release(); } catch (Throwable ignored) {}
        }

        stopSelf();
    }

    public void handleHeadsetDisconnected() {
        isPlaying = false;
        updateSystemPlaybackState();
        postForegroundNotification();
        if (listener != null) {
            listener.onHeadsetDisconnected();
        }
    }

    private void loadArtworkAsync(final String thumbUrl) {
        if (thumbUrl != null && !thumbUrl.isEmpty()) {
            new Thread(() -> {
                try {
                    URL url = new URL(thumbUrl);
                    Bitmap bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                    if (bmp != null) {
                        currentBitmap = bmp;
                        postForegroundNotification();
                        updateSystemMetadata();
                    }
                } catch (Throwable ignored) {}
            }).start();
        }
    }

    private void updateSystemMetadata() {
        if (mediaSession == null) return;
        try {
            MediaMetadata.Builder metaBuilder = new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, currentTitle)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, currentArtist + (currentViews.isEmpty() ? "" : " • " + currentViews))
                .putString(MediaMetadata.METADATA_KEY_ALBUM, "8D Audio")
                .putLong(MediaMetadata.METADATA_KEY_DURATION, currentDurationMs);

            if (currentBitmap != null) {
                metaBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, currentBitmap);
                metaBuilder.putBitmap(MediaMetadata.METADATA_KEY_ART, currentBitmap);
            }
            mediaSession.setMetadata(metaBuilder.build());
        } catch (Throwable ignored) {}
    }

    private void updateSystemPlaybackState() {
        if (mediaSession == null) return;
        try {
            int state = isPlaying ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED;
            float speed = isPlaying ? 1.0f : 0.0f;
            long actions = PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE |
                           PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_STOP |
                           PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS |
                           PlaybackState.ACTION_REWIND | PlaybackState.ACTION_FAST_FORWARD |
                           PlaybackState.ACTION_SEEK_TO;

            PlaybackState.Builder stateBuilder = new PlaybackState.Builder()
                .setActions(actions)
                .setState(state, currentPositionMs, speed, SystemClock.elapsedRealtime());
            mediaSession.setPlaybackState(stateBuilder.build());

            if (wakeLock != null) {
                if (isPlaying) {
                    if (!wakeLock.isHeld()) {
                        wakeLock.acquire(120 * 60 * 1000L); // 2 hours
                    }
                } else {
                    if (wakeLock.isHeld()) {
                        wakeLock.release();
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    private void postForegroundNotification() {
        try {
            updateSystemPlaybackState();

            Intent openAppIntent = new Intent(this, MainActivity.class);
            openAppIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent openPending = PendingIntent.getActivity(this, 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            Notification.Builder builder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder = new Notification.Builder(this, CHANNEL_ID);
            } else {
                builder = new Notification.Builder(this);
            }

            builder.setSmallIcon(R.drawable.icon)
                .setContentTitle(currentTitle != null ? currentTitle : "8D Audio")
                .setContentText((currentArtist != null && !currentArtist.isEmpty()) ? (currentArtist + " • 8D Audio") : "8D Audio Playing")
                .setContentIntent(openPending)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOngoing(isPlaying);

            if (currentBitmap != null) {
                builder.setLargeIcon(currentBitmap);
            }

            Notification notification = builder.build();

            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (isPlaying) {
                startForeground(NOTIFICATION_ID, notification);
            } else {
                stopForeground(true);
                if (nm != null) {
                    nm.cancel(NOTIFICATION_ID);
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static boolean isHeadphonesConnected(Context ctx) {
        if (ctx == null) return true;
        try {
            AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
            if (am == null) return true;

            // 1. Direct AudioManager state (legacy check)
            if (am.isWiredHeadsetOn() || am.isBluetoothA2dpOn() || am.isBluetoothScoOn()) {
                return true;
            }

            // 2. AudioDeviceInfo (API 23+) - physical headphones, bluetooth & USB-C audio
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AudioDeviceInfo[] devices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
                for (AudioDeviceInfo dev : devices) {
                    int type = dev.getType();
                    if (type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||     // 3
                        type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||  // 4
                        type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||    // 8
                        type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||     // 7
                        type == AudioDeviceInfo.TYPE_USB_HEADSET ||       // 22
                        type == AudioDeviceInfo.TYPE_USB_DEVICE ||        // 11 (Type-C earphones/DAC)
                        type == AudioDeviceInfo.TYPE_HEARING_AID ||       // 23
                        type == 26 ||                                     // TYPE_BLE_HEADSET
                        type == 27 ||                                     // TYPE_BLE_SPEAKER
                        type == 30) {                                     // TYPE_BLE_BROADCAST
                        return true;
                    }
                }
            }

            // 3. BluetoothAdapter active profile check
            try {
                BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
                if (btAdapter != null && btAdapter.isEnabled()) {
                    int a2dpState = btAdapter.getProfileConnectionState(BluetoothProfile.A2DP);
                    int headsetState = btAdapter.getProfileConnectionState(BluetoothProfile.HEADSET);
                    if (a2dpState == BluetoothProfile.STATE_CONNECTED ||
                        headsetState == BluetoothProfile.STATE_CONNECTED) {
                        return true;
                    }
                }
            } catch (Throwable ignored) {}

            return false;
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        if (disconnectReceiver != null) {
            try {
                unregisterReceiver(disconnectReceiver);
            } catch (Throwable ignored) {}
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            try {
                wakeLock.release();
            } catch (Throwable ignored) {}
        }
        if (mediaSession != null) {
            try {
                mediaSession.setActive(false);
                mediaSession.release();
            } catch (Throwable ignored) {}
        }
    }
}
