#!/usr/bin/env bash
set -euo pipefail
TEST_CLASSES="$(mktemp -d)"
trap 'rm -rf "$TEST_CLASSES"' EXIT
javac -d "$TEST_CLASSES" tools/dynamic/FullBlockRules.java tools/tests/FullBlockRulesTest.java
java -cp "$TEST_CLASSES" FullBlockRulesTest
