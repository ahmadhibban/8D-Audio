package com.hits100m.app;

import android.app.Activity;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    public static class BackgroundAudioWebView extends WebView {
        public BackgroundAudioWebView(Context context) {
            super(context);
        }

        @Override
        protected void onWindowVisibilityChanged(int visibility) {
            super.onWindowVisibilityChanged(android.view.View.VISIBLE);
        }

        @Override
        public void onWindowFocusChanged(boolean hasWindowFocus) {
            super.onWindowFocusChanged(true);
        }
    }

    private WebView webView;
    private PowerManager.WakeLock wakeLock;
    private BroadcastReceiver disconnectReceiver;

    private static final String[] DEFAULT_QUERIES = {
        "8D audio songs",
        "9D audio songs",
        "10D audio songs",
        "8D music mix hits",
        "9D audio binaural",
        "10D audio headphones mix",
        "8D popular songs",
        "8D english hits",
        "8D hindi songs"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        } catch (Throwable ignored) {}

        initWakeLock();
        initRemoteListener();
        initDisconnectReceiver();

        try {
            webView = new BackgroundAudioWebView(this);
            setContentView(webView);

            WebSettings ws = webView.getSettings();
            ws.setJavaScriptEnabled(true);
            ws.setDomStorageEnabled(true);
            ws.setAllowFileAccess(true);
            ws.setAllowContentAccess(true);
            ws.setMediaPlaybackRequiresUserGesture(false);
            ws.setDatabaseEnabled(true);
            ws.setUseWideViewPort(true);
            ws.setLoadWithOverviewMode(true);
            ws.setSupportZoom(false);
            ws.setUserAgentString("Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");

            webView.addJavascriptInterface(new WebAppInterface(), "AndroidMedia");

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    view.loadUrl(url);
                    return true;
                }
            });

            webView.setWebChromeClient(new WebChromeClient());

            try {
                InputStream is = getAssets().open("index.html");
                byte[] buffer = new byte[is.available()];
                is.read(buffer);
                is.close();
                String html = new String(buffer, "UTF-8");
                webView.loadDataWithBaseURL("https://localhost/", html, "text/html", "UTF-8", null);
            } catch (Throwable e) {
                webView.loadUrl("file:///android_asset/index.html");
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void initRemoteListener() {
        MediaPlaybackService.setListener(new MediaPlaybackService.RemoteControlListener() {
            @Override public void onRemoteToggle() { runOnUiThread(() -> { if (webView != null) webView.evaluateJavascript("if(window.togglePlay) window.togglePlay();", null); }); }
            @Override public void onRemoteNext() { runOnUiThread(() -> { if (webView != null) webView.evaluateJavascript("if(window.playNext) window.playNext();", null); }); }
            @Override public void onRemotePrev() { runOnUiThread(() -> { if (webView != null) webView.evaluateJavascript("if(window.playPrev) window.playPrev();", null); }); }
            @Override public void onRemoteRewind10() {}
            @Override public void onRemoteForward10() {}
            @Override public void onRemoteSeekTo(long posMs) {}
            @Override public void onRemoteStop() { runOnUiThread(() -> { if (webView != null) webView.evaluateJavascript("if(window.pauseAudio) window.pauseAudio();", null); }); }
            @Override public void onHeadsetDisconnected() {
                runOnUiThread(() -> {
                    if (webView != null) {
                        webView.evaluateJavascript("if(window.pauseAudio) window.pauseAudio();", null);
                    }
                });
            }
        });
    }

    private void initDisconnectReceiver() {
        try {
            disconnectReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent == null) return;
                    String action = intent.getAction();
                    if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(action) ||
                        BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                        pauseAudioDirect();
                    } else if (Intent.ACTION_HEADSET_PLUG.equals(action)) {
                        int state = intent.getIntExtra("state", -1);
                        if (state == 0) {
                            pauseAudioDirect();
                        }
                    } else if (BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED.equals(action) ||
                               BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
                        int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);
                        if (state == BluetoothProfile.STATE_DISCONNECTED) {
                            pauseAudioDirect();
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

    private void pauseAudioDirect() {
        runOnUiThread(() -> {
            if (webView != null) {
                webView.evaluateJavascript("if(window.pauseAudio) window.pauseAudio();", null);
            }
        });
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

    public class WebAppInterface {
        @JavascriptInterface
        public void onStateChanged(boolean isPlaying) {
            runOnUiThread(() -> {
                if (wakeLock != null) {
                    try {
                        if (isPlaying) {
                            if (!wakeLock.isHeld()) {
                                wakeLock.acquire(120 * 60 * 1000L); // 2 hours
                            }
                        } else {
                            if (wakeLock.isHeld()) {
                                wakeLock.release();
                            }
                        }
                    } catch (Throwable ignored) {}
                }

                try {
                    Intent serviceIntent = new Intent(MainActivity.this, MediaPlaybackService.class);
                    serviceIntent.setAction(MediaPlaybackService.ACTION_UPDATE_STATE);
                    serviceIntent.putExtra("isPlaying", isPlaying);
                    if (isPlaying) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(serviceIntent);
                        } else {
                            startService(serviceIntent);
                        }
                    } else {
                        startService(serviceIntent);
                    }
                } catch (Throwable ignored) {}
            });
        }

        @JavascriptInterface
        public void onTrackChanged(String title, String artist, String thumbUrl) {
            runOnUiThread(() -> {
                try {
                    Intent serviceIntent = new Intent(MainActivity.this, MediaPlaybackService.class);
                    serviceIntent.setAction(MediaPlaybackService.ACTION_UPDATE_TRACK);
                    serviceIntent.putExtra("title", title);
                    serviceIntent.putExtra("artist", artist);
                    serviceIntent.putExtra("thumbUrl", thumbUrl);
                    serviceIntent.putExtra("isPlaying", true);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent);
                    } else {
                        startService(serviceIntent);
                    }
                } catch (Throwable ignored) {}
            });
        }

        @JavascriptInterface
        public void search8DOnline(String query, int page, boolean isRefresh) {
            new Thread(() -> {
                try {
                    String q = (query != null && !query.trim().isEmpty()) ? query.trim() : DEFAULT_QUERIES[page % DEFAULT_QUERIES.length];
                    if (!Pattern.compile("\\b([89]|10)\\s*D\\b", Pattern.CASE_INSENSITIVE).matcher(q).find()) {
                        q = q + " 8D audio";
                    }

                    String urlStr = "https://www.youtube.com/results?search_query=" + URLEncoder.encode(q, "UTF-8");
                    URL u = new URL(urlStr);
                    HttpURLConnection conn = (HttpURLConnection) u.openConnection();
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
                    conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
                    conn.setConnectTimeout(6000);
                    conn.setReadTimeout(6000);

                    Scanner scanner = new Scanner(conn.getInputStream(), "UTF-8").useDelimiter("\\A");
                    String resp = scanner.hasNext() ? scanner.next() : "";
                    scanner.close();

                    Pattern pData = Pattern.compile("var ytInitialData\\s*=\\s*(\\{.*?\\});</script>");
                    Matcher mData = pData.matcher(resp);
                    if (mData.find()) {
                        String jsonStr = mData.group(1);
                        runOnUiThread(() -> {
                            if (webView != null) {
                                webView.evaluateJavascript("if(window.onRawYtDataReceived) window.onRawYtDataReceived(" + jsonStr + ", " + isRefresh + ");", null);
                            }
                        });
                    }
                } catch (Throwable t) {
                    runOnUiThread(() -> {
                        if (webView != null) {
                            webView.evaluateJavascript("if(window.onSearchFailed) window.onSearchFailed();", null);
                        }
                    });
                }
            }).start();
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null) {
            webView.evaluateJavascript("if(window.isFullPlayerOpen && window.isFullPlayerOpen()){ window.closeFullPlayer(); 'true'; } else { 'false'; }", value -> {
                if ("\"true\"".equals(value) || "true".equals(value)) {
                    // Closed full-screen player, stay in app
                } else {
                    // Minimize to background and keep playing smoothly
                    moveTaskToBack(true);
                }
            });
        } else {
            moveTaskToBack(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
    }
}
