#!/usr/bin/env bash
set -euo pipefail

BASE_APK="${1:-input/V2F.apk}"
OUT_APK="${2:-build/V2F-dynamic-labels.apk}"
ROOT="build/decompiled"
APKTOOL="build/tools/apktool.jar"

rm -rf "$ROOT" build/dynamic-dex build/dynamic-classes
mkdir -p build/tools

if [ ! -f "$APKTOOL" ]; then
  curl -fL --retry 3 -o "$APKTOOL" https://github.com/iBotPeaches/Apktool/releases/download/v2.10.0/apktool_2.10.0.jar
fi

java -jar "$APKTOOL" d -f "$BASE_APK" -o "$ROOT"
python3 tools/patch_package_name.py "$ROOT"
python3 tools/patch_dynamic_labels.py "$ROOT"
python3 tools/patch_dynamic_guard.py "$ROOT"
python3 tools/patch_break_interval_dynamic.py "$ROOT"
bash tools/build_dynamic_dex.sh
java -jar "$APKTOOL" b "$ROOT" -o build/V2F-unsigned.apk
python3 tools/package_dynamic.py build/V2F-unsigned.apk build/classes15.dex

SDK_ROOT="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/usr/local/lib/android/sdk}}"
BUILD_TOOLS="$(find "$SDK_ROOT/build-tools" -mindepth 1 -maxdepth 1 -type d | sort -V | tail -n 1)"
"$BUILD_TOOLS/zipalign" -f -p 4 build/V2F-unsigned.apk build/V2F-aligned.apk

rm -f build/signing.jks
if [ -n "${APK_KEYSTORE_B64:-}" ]; then
  printf '%s' "$APK_KEYSTORE_B64" | base64 -d > build/signing.jks
  KS_ALIAS="${APK_KEY_ALIAS:?Set APK_KEY_ALIAS secret}"
  KS_PASS="${APK_KEYSTORE_PASSWORD:?Set APK_KEYSTORE_PASSWORD secret}"
  KEY_PASS="${APK_KEY_PASSWORD:?Set APK_KEY_PASSWORD secret}"
  echo 'Using repository signing key.'
else
  keytool -genkeypair -v \
    -keystore build/signing.jks \
    -storepass android \
    -keypass android \
    -alias v2f-ci \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -dname 'CN=V2F CI,O=VX,C=US'
  KS_ALIAS='v2f-ci'
  KS_PASS='android'
  KEY_PASS='android'
  echo 'Repository signing key is not configured; using temporary CI key.'
fi

"$BUILD_TOOLS/apksigner" sign \
  --ks build/signing.jks \
  --ks-key-alias "$KS_ALIAS" \
  --ks-pass "pass:$KS_PASS" \
  --key-pass "pass:$KEY_PASS" \
  --v2-signing-enabled true \
  --v3-signing-enabled true \
  --out "$OUT_APK" \
  build/V2F-aligned.apk
"$BUILD_TOOLS/apksigner" verify --verbose --print-certs "$OUT_APK"
"$BUILD_TOOLS/zipalign" -c -v 4 "$OUT_APK"
unzip -l "$OUT_APK" | grep -q 'classes15.dex'

echo "Built: $OUT_APK"
