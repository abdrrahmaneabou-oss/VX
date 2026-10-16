#!/usr/bin/env python3
from pathlib import Path
import re
import sys

def main():
    root = Path(sys.argv[1])
    print("=== V2F state symbols ===", flush=True)
    for p in root.rglob("*.smali"):
        text = p.read_text(encoding="utf-8", errors="ignore")
        if "Lcom/v1f/app/patch/BreakRules$State;" in text and p.name in ("BreakRules$State.smali", "BreakRules.smali"):
            print(f"--- {p} ---", flush=True)
            for line in text.splitlines():
                if line.startswith(".field") or line.startswith(".method"):
                    print(line, flush=True)
            for i, line in enumerate(text.splitlines(), 1):
                if "BreakRules$State;->" in line or "BreakRules;->at" in line:
                    print(f"{i}: {line}", flush=True)
    print("=== likely strict-mode names and persisted keys ===", flush=True)
    patterns = re.compile(r"strict.?block|hard.?block|block.?mode|strict.?mode|blocking.?enabled|strict_block|hard_block|block_mode|strict_mode", re.I)
    count = 0
    for p in root.rglob("*.smali"):
        lines = p.read_text(encoding="utf-8", errors="ignore").splitlines()
        for i, line in enumerate(lines):
            if patterns.search(line):
                print(f"{p}:{i+1}: {line.strip()}", flush=True)
                count += 1
                if count >= 100:
                    print("...truncated...", flush=True)
                    return 0
    if count == 0:
        print("No literal strict-mode identifiers found.", flush=True)
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
