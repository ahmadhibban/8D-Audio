#!/bin/bash
# 8D Audio Player - One-Click Build Script (Termux / Linux)
# Author: Ahmad Hibban
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$DIR/../android_project"
if [ ! -d "$PROJECT_DIR" ]; then
    PROJECT_DIR="$DIR"
fi

cd "$PROJECT_DIR"
echo "==> Building in: $PROJECT_DIR"

ANDROID_JAR="/data/data/com.termux/files/home/android-sdk-jar/android.jar"
if [ ! -f "$ANDROID_JAR" ]; then
    echo "ERROR: android.jar not found at $ANDROID_JAR"
    exit 1
fi

rm -rf bin gen
mkdir -p bin gen

echo "==> 1. Generating R.java..."
aapt package -f -m -J gen/ -M AndroidManifest.xml -S res/ -I "$ANDROID_JAR"

echo "==> 2. Compiling Java..."
javac -d bin/ -cp "$ANDROID_JAR" gen/com/hits100m/app/R.java src/com/hits100m/app/*.java

echo "==> 3. Dexing classes..."
d8 --lib "$ANDROID_JAR" --output bin/ $(find bin/ -name "*.class")

echo "==> 4. Packaging APK..."
aapt package -f -M AndroidManifest.xml -S res/ -A assets/ -I "$ANDROID_JAR" -F bin/app.unsigned.apk
cd bin
aapt add app.unsigned.apk classes.dex
cd ..

echo "==> 5. Signing APK..."
OUT_APK="$DIR/../apk/8D_Audio.apk"
mkdir -p "$DIR/../apk"
apksigner sign --ks debug.keystore --ks-pass pass:android --out "$OUT_APK" bin/app.unsigned.apk

echo "==> 6. Verifying signature..."
apksigner verify -v "$OUT_APK"

echo "SUCCESS! APK ready at: $OUT_APK"
