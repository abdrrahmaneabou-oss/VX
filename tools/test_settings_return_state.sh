#!/usr/bin/env bash
set -euo pipefail
TEMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TEMP_DIR"' EXIT
javac -source 8 -target 8 -d "$TEMP_DIR" \
    tools/dynamic/SettingsReturnState.java \
    tools/tests/SettingsReturnStateTest.java
java -cp "$TEMP_DIR" SettingsReturnStateTest
