#!/usr/bin/env python3
"""Replace the fixed break interval literal with the persisted runtime setting."""
from pathlib import Path
import re
import sys

TARGET_CLASS = "Lcom/v1f/app/patch/BreakRules;"
INTERVAL_HEX = "0x2932e00"  # 43,200,000 ms (12 hours; the approved APK base literal)
INTERVAL_DECIMAL = "43200000"
STORE = "Lcom/v1f/app/patch/BreakIntervalStore;"


def main() -> int:
    if len(sys.argv) != 2:
        print("Usage: patch_break_interval_dynamic.py <apktool-output-dir>", file=sys.stderr)
        return 2

    root = Path(sys.argv[1])
    candidates = [
        p for p in root.rglob("BreakRules.smali")
        if TARGET_CLASS in p.read_text(encoding="utf-8", errors="ignore")
    ]
    if len(candidates) != 1:
        raise RuntimeError(f"Expected exactly one BreakRules.smali, found {len(candidates)}")

    path = candidates[0]
    text = path.read_text(encoding="utf-8")
    pattern = re.compile(
        rf"(?m)^(?P<indent>\s*)(?P<op>const-wide(?:/\d+)?|const-wide)\s+"
        rf"(?P<reg>v\d+),\s*(?:{re.escape(INTERVAL_HEX)}|{INTERVAL_DECIMAL})\s*$",
        re.IGNORECASE,
    )

    def replace(match: re.Match) -> str:
        indent = match.group("indent")
        reg = match.group("reg")
        return (
            f"{indent}invoke-static {{}}, {STORE}->getIntervalMs()J\n"
            f"{indent}move-result-wide {reg}"
        )

    patched, count = pattern.subn(replace, text)
    if count < 1:
        print("BreakRules candidates:", file=sys.stderr)
        for number, line in enumerate(text.splitlines(), 1):
            if "const-wide" in line or "86400000" in line or "5265c00" in line.lower():
                print(f"{number}: {line}", file=sys.stderr)
        print("Matching interval literals anywhere in smali:", file=sys.stderr)
        for other in root.rglob("*.smali"):
            for number, line in enumerate(other.read_text(encoding="utf-8", errors="ignore").splitlines(), 1):
                if "86400000" in line or "5265c00" in line.lower():
                    print(f"{other}:{number}: {line}", file=sys.stderr)
        raise RuntimeError("Could not find the approved APK interval literal; diagnostics above identify the actual bytecode representation.")
    if pattern.search(patched):
        raise RuntimeError("A fixed interval const-wide instruction remains in BreakRules")

    path.write_text(patched, encoding="utf-8")
    print(f"Patched {path}: {count} fixed interval reference(s) now use {STORE}->getIntervalMs()J")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
