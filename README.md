# 8D Audio - Spatial Audio Music Streaming Android App

An Android music streaming application designed specifically for 8D, 9D, and 10D spatial audio tracks. Built with a Spotify and Apple Music inspired mobile interface, continuous background playback, and automatic pause on Bluetooth disconnection.

## ✨ Features

- **Spatial 8D Audio Streaming**: High-definition audio playback engine optimized for binaural and multi-dimensional audio.
- **Modern In-App Player**:
  - **Docked Mini-Player**: Bottom mini-player with live progress fill, track metadata, and quick controls.
  - **Full-Screen Modal**: Slide-up player featuring ambient glow artwork, interactive seekbar with live elapsed/remaining timestamps, and prominent playback controls.
  - **Double-Tap 10s Seek**: Double-tap the left side of the album artwork to rewind 10s or the right side to jump forward 10s.
  - **Up-Next Queue Drawer**: View upcoming tracks and switch instantly.
- **Continuous Background Playback**:
  - Runs with a lightweight Android Foreground Service and CPU WakeLock.
  - Keeps playing smoothly when switching apps, returning to the home screen, or locking the device.
- **Bluetooth Disconnect Protection**:
  - Automatically pauses playback immediately if Bluetooth earbuds or headphones disconnect.
- **Clean Vector UI**:
  - 100% crisp vector SVG icons.

## 📱 Prebuilt APK

The signed, ready-to-install APK is located at:
`apk/8D_Audio.apk`

## 🛠️ Build from Source (Termux / Linux)

Prerequisites:
- Android SDK build tools (`aapt`, `d8`, `apksigner`)
- `android.jar` (API 28+)
- Java compiler (`javac`)

To build the APK:
```bash
bash scripts/build_apk.sh
```
The output APK will be placed in the `apk/` directory.

## 📄 License

Open-source under the MIT License.
