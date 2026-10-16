#!/usr/bin/env python3
from pathlib import Path
import re
import sys

def main():
    root = Path(sys.argv[1])
    candidates = list(root.rglob("BreakRules.smali"))
    print("=== V2F strict/block state diagnostics ===", flush=True)
    if len(candidates) != 1:
        print(f"BreakRules candidates: {len(candidates)}", flush=True)
        return 0
    text = candidates[0].read_text(encoding="utf-8", errors="ignore")
    for line in text.splitlines():
        if line.startswith(".field") or line.startswith(".method"):
            print(line, flush=True)
    print("=== likely mode/state references ===", flush=True)
    for i, line in enumerate(text.splitlines(), 1):
        if re.search(r"strict|block|active|enabled|break|state|running|pref|SharedPreferences", line, re.I):
            print(f"{i}: {line}", flush=True)
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
