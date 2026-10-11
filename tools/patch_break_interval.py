#!/usr/bin/env python3
from pathlib import Path
import re
import sys

TARGET_CLASS = "Lcom/v1f/app/patch/BreakRules;"
OLD_MS = 86_400_000
NEW_MS = 43_200_000
OLD_HEX = f"0x{OLD_MS:x}"
NEW_HEX = f"0x{NEW_MS:x}"


def main() -> int:
    if len(sys.argv) != 2:
        print("Usage: patch_break_interval.py <apktool-output-dir>", file=sys.stderr)
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

    # Kotlin/Smali normally emits the 24-hour interval as a hex literal.
    # Accept either the hexadecimal or decimal representation, but patch
    # exactly one occurrence inside BreakRules so unrelated constants remain untouched.
    patterns = [
        (re.compile(rf"(?i)(?<![0-9a-f]){re.escape(OLD_HEX)}(?![0-9a-f])"), NEW_HEX),
        (re.compile(rf"(?<!\d){OLD_MS}(?!\d)"), str(NEW_MS)),
    ]

    total = 0
    patched = text
    for pattern, replacement in patterns:
        patched, count = pattern.subn(replacement, patched)
        total += count

    if total != 1:
        raise RuntimeError(
            f"Expected exactly one 24-hour interval constant in BreakRules, replaced {total}"
        )

    if OLD_HEX.lower() in patched.lower() or str(OLD_MS) in patched:
        raise RuntimeError("24-hour interval still present after patch")
    if NEW_HEX.lower() not in patched.lower() and str(NEW_MS) not in patched:
        raise RuntimeError("12-hour interval missing after patch")

    path.write_text(patched, encoding="utf-8")
    print(f"Patched {path}")
    print(f"Break interval: {OLD_MS} ms -> {NEW_MS} ms (24h -> 12h)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
