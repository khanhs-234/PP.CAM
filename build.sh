#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"
ROOT="$PWD"

# Toolchain paths
: "${JAVA_HOME:=/opt/homebrew/opt/openjdk@17}"
: "${ANDROID_SDK:=$HOME/Android/Sdk}"
: "${BUILD_TOOLS:=$ANDROID_SDK/build-tools/30.0.3}"
: "${PLATFORM_JAR:=$ANDROID_SDK/platforms/android-28/android.jar}"

export PATH="$JAVA_HOME/bin:$PATH"

echo "=== [1/6] Dọn dẹp thư mục build ==="
rm -rf out
mkdir -p out/gen out/classes out/dex out/lib/armeabi

echo "=== [2/6] Biên dịch Resources (aapt R.java) ==="
"$BUILD_TOOLS/aapt" package -f -m -J out/gen -M AndroidManifest.xml -S res -I "$PLATFORM_JAR"

echo "=== [3/6] Biên dịch mã nguồn Java (javac) ==="
find out/gen src -name "*.java" > out/sources.txt
"$JAVA_HOME/bin/javac" -encoding UTF-8 -source 8 -target 8 -Xlint:-options -cp "$PLATFORM_JAR" -d out/classes @"out/sources.txt"

echo "=== [4/6] Chuyển đổi sang Bytecode Dalvik (d8) ==="
find out/classes -name "*.class" > out/classes.txt
"$JAVA_HOME/bin/java" -cp "$BUILD_TOOLS/lib/d8.jar" com.android.tools.r8.D8 \
  --release --min-api 10 --lib "$PLATFORM_JAR" --output out/dex @"out/classes.txt"

echo "=== [5/6] Đóng gói APK và nhúng Thư viện Native JNI ==="
"$BUILD_TOOLS/aapt" package -f -M AndroidManifest.xml -S res -I "$PLATFORM_JAR" -F out/unaligned.apk
( cd out/dex && "$BUILD_TOOLS/aapt" add ../unaligned.apk classes.dex )

cp lib/armeabi/librecipelab.so out/lib/armeabi/
( cd out && "$BUILD_TOOLS/aapt" add unaligned.apk lib/armeabi/librecipelab.so )

"$BUILD_TOOLS/zipalign" -f 4 out/unaligned.apk out/aligned.apk

echo "=== [6/6] Ký số APK chuẩn v1 (Sony PMCA Camera Compatible) ==="
if [ ! -f debug.keystore ]; then
  "$JAVA_HOME/bin/keytool" -genkeypair -keystore debug.keystore -alias ppcam \
    -keyalg RSA -keysize 2048 -validity 10000 -storepass android -keypass android -dname "CN=PPCAM"
fi

"$BUILD_TOOLS/apksigner" sign --ks debug.keystore --ks-pass pass:android --key-pass pass:android \
  --min-sdk-version 10 --v1-signing-enabled true --v2-signing-enabled false \
  --v3-signing-enabled false --out PPCAM-dev.apk out/aligned.apk

"$BUILD_TOOLS/apksigner" verify --min-sdk-version 10 PPCAM-dev.apk
sha256sum PPCAM-dev.apk 2>/dev/null || shasum -a 256 PPCAM-dev.apk

echo "=========================================================="
echo " BUILD THÀNH CÔNG: $ROOT/PPCAM-dev.apk"
echo "=========================================================="
