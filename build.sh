#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"
ROOT="$PWD"

# 1. Detect JAVA_HOME
if [ -z "${JAVA_HOME:-}" ]; then
  if [ -d "/opt/homebrew/opt/openjdk@17" ]; then
    JAVA_HOME="/opt/homebrew/opt/openjdk@17"
  elif command -v javac >/dev/null 2>&1; then
    JAVA_HOME="$(dirname "$(dirname "$(command -v javac)")")"
  fi
fi

# 2. Detect Android SDK (Supports GitHub Actions, macOS, and Linux)
: "${ANDROID_SDK:=${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/Android/Sdk}}}"
: "${BUILD_TOOLS_VERSION:=30.0.3}"
BT="$ANDROID_SDK/build-tools/$BUILD_TOOLS_VERSION"
: "${PLATFORM_JAR:=$ANDROID_SDK/platforms/android-28/android.jar}"

if [ -n "${JAVA_HOME:-}" ]; then
  JAVA="$JAVA_HOME/bin"
  export PATH="$JAVA:$PATH"
else
  JAVA="$(dirname "$(command -v javac)")"
fi

echo "Toolchain info:"
echo "  JAVA_HOME   : ${JAVA_HOME:-default}"
echo "  ANDROID_SDK : $ANDROID_SDK"
echo "  BUILD_TOOLS : $BT"
echo "  PLATFORM_JAR: $PLATFORM_JAR"

# Verify required files
for f in "$PLATFORM_JAR" "$BT/aapt" "$BT/zipalign" "$BT/apksigner" "$BT/lib/d8.jar"; do
  [ -e "$f" ] || { echo "Lỗi thiếu công cụ: $f" >&2; exit 1; }
done

echo "=== [1/6] Dọn dẹp thư mục build ==="
rm -rf out
mkdir -p out/gen out/classes out/dex out/lib/armeabi

echo "=== [2/6] Biên dịch Resources (aapt R.java) ==="
"$BT/aapt" package -f -m -J out/gen -M AndroidManifest.xml -S res -I "$PLATFORM_JAR"

echo "=== [3/6] Biên dịch mã nguồn Java (javac) ==="
find out/gen src -name "*.java" > out/sources.txt
"$JAVA/javac" -encoding UTF-8 -source 8 -target 8 -Xlint:-options -cp "$PLATFORM_JAR" -d out/classes @"out/sources.txt"

echo "=== [4/6] Chuyển đổi sang Bytecode Dalvik (d8) ==="
find out/classes -name "*.class" > out/classes.txt
"$JAVA/java" -cp "$BT/lib/d8.jar" com.android.tools.r8.D8 \
  --release --min-api 10 --lib "$PLATFORM_JAR" --output out/dex @"out/classes.txt"

echo "=== [5/6] Đóng gói APK và nhúng Thư viện Native JNI ==="
"$BT/aapt" package -f -M AndroidManifest.xml -S res -I "$PLATFORM_JAR" -F out/unaligned.apk
( cd out/dex && "$BT/aapt" add ../unaligned.apk classes.dex )

cp lib/armeabi/librecipelab.so out/lib/armeabi/
( cd out && "$BT/aapt" add unaligned.apk lib/armeabi/librecipelab.so )

"$BT/zipalign" -f 4 out/unaligned.apk out/aligned.apk

echo "=== [6/6] Ký số APK chuẩn v1 (Sony PMCA Camera Compatible) ==="
if [ ! -f debug.keystore ]; then
  "$JAVA/keytool" -genkeypair -keystore debug.keystore -alias ppcam \
    -keyalg RSA -keysize 2048 -validity 10000 -storepass android -keypass android -dname "CN=PPCAM"
fi

"$BT/apksigner" sign --ks debug.keystore --ks-pass pass:android --key-pass pass:android \
  --min-sdk-version 10 --v1-signing-enabled true --v2-signing-enabled false \
  --v3-signing-enabled false --out PPCAM-dev.apk out/aligned.apk

"$BT/apksigner" verify --min-sdk-version 10 PPCAM-dev.apk
sha256sum PPCAM-dev.apk 2>/dev/null || shasum -a 256 PPCAM-dev.apk

echo "=========================================================="
echo " BUILD THÀNH CÔNG: $ROOT/PPCAM-dev.apk"
echo "=========================================================="
