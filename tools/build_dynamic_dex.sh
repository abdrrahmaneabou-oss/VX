#!/usr/bin/env bash
set -euo pipefail

SDK_ROOT="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/usr/local/lib/android/sdk}}"
BUILD_TOOLS="$(find "$SDK_ROOT/build-tools" -mindepth 1 -maxdepth 1 -type d | sort -V | tail -n 1)"
ANDROID_JAR="$(find "$SDK_ROOT/platforms" -mindepth 2 -maxdepth 2 -name android.jar | sort -V | tail -n 1)"

test -x "$BUILD_TOOLS/d8"
test -f "$ANDROID_JAR"

rm -rf build/dynamic-classes build/dynamic-dex
mkdir -p build/dynamic-classes build/dynamic-dex

javac -source 8 -target 8 \
  -cp "$ANDROID_JAR" \
  -d build/dynamic-classes \
  tools/dynamic/stubs/com/v1f/app/protection/modules/settings/SettingsClickGuardConfig.java \
  tools/dynamic/DynamicLabelsStore.java \
  tools/dynamic/BreakIntervalStore.java \
  tools/dynamic/StrictBlockPolicy.java \
  tools/dynamic/RandomFullBlock.java \
  tools/dynamic/FullBlockRules.java \
  tools/dynamic/FullBlockCountdownView.java \
  tools/dynamic/FullBlockSettings.java \
  tools/dynamic/FullBlockExemptionsActivity.java \
  tools/dynamic/DefaultSelections.java \
  tools/dynamic/GuardBridgeGrace.java \
  tools/dynamic/DynamicLabelsActivity.java \
  tools/dynamic/DynamicLabelsEntry.java \
  tools/dynamic/HomeCardState.java \
  tools/dynamic/RobustSettingsGuard.java \
  tools/dynamic/stubs/com/v1f/app/protection/overlay/MeteorShieldOverlay.java \
  tools/dynamic/SettingsBackCoordinator.java \
  tools/dynamic/SettingsReturnState.java \
  tools/dynamic/SettingsReturnEngine.java

# The stub only exists to satisfy javac. The real class is already in classes4.dex.
rm -rf build/dynamic-classes/com/v1f/app/protection/modules/settings
rm -rf build/dynamic-classes/com/v1f/app/protection/overlay

mapfile -t CLASSES < <(find build/dynamic-classes -name '*.class' -print)
if [ "${#CLASSES[@]}" -eq 0 ]; then
  echo 'No dynamic label classes were compiled' >&2
  exit 1
fi

"$BUILD_TOOLS/d8" \
  --lib "$ANDROID_JAR" \
  --min-api 23 \
  --output build/dynamic-dex \
  "${CLASSES[@]}"

test -f build/dynamic-dex/classes.dex
cp build/dynamic-dex/classes.dex build/classes15.dex

echo "Dynamic label manager DEX ready: $(du -h build/classes15.dex | cut -f1)"
