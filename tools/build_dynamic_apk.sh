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
python3 tools/inspect_block_state.py "$ROOT"
# Diagnostic only; this script does not alter strict-block state.
# Updated hourly scheduler integration.
# The native protection action is inserted before the optional actions in HomeScreen.
# The protection-list Activity uses the redesigned celestial dark settings interface.
# Enhanced editor includes live unsaved-state feedback and reset confirmation.
python3 tools/patch_dynamic_labels.py "$ROOT"
python3 tools/patch_dynamic_guard.py "$ROOT"
python3 tools/patch_break_interval_dynamic.py "$ROOT"
python3 tools/patch_three_second_lock.py "$ROOT"
python3 tools/patch_native_protection_list.py "$ROOT"
python3 tools/patch_accessibility_only.py "$ROOT"
python3 tools/patch_random_full_block.py "$ROOT"
bash tools/build_dynamic_dex.sh
java -jar "$APKTOOL" b "$ROOT" -o build/V2F-unsigned.apk
python3 tools/package_dynamic.py build/V2F-unsigned.apk build/classes15.dex

SDK_ROOT="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/usr/local/lib/android/sdk}}"
BUILD_TOOLS="$(find "$SDK_ROOT/build-tools" -mindepth 1 -maxdepth 1 -type d | sort -V | tail -n 1)"
"$BUILD_TOOLS/zipalign" -f -p 4 build/V2F-unsigned.apk build/V2F-aligned.apk

keytool -genkeypair -v -keystore build/signing.jks -storepass android -keypass android -alias v2f-ci -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=V2F CI,O=VX,C=US'

"$BUILD_TOOLS/apksigner" sign --ks build/signing.jks --ks-key-alias v2f-ci --ks-pass pass:android --key-pass pass:android --v2-signing-enabled true --v3-signing-enabled true --out "$OUT_APK" build/V2F-aligned.apk
"$BUILD_TOOLS/apksigner" verify --verbose "$OUT_APK"
"$BUILD_TOOLS/zipalign" -c -v 4 "$OUT_APK"
unzip -l "$OUT_APK" | grep -q 'classes15.dex'

echo "Built: $OUT_APK"
