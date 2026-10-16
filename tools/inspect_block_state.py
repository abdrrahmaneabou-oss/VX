#!/usr/bin/env python3
from pathlib import Path
import re
import sys

def main():
    root = Path(sys.argv[1])
    print("=== BreakRuntime/BreakRules class fields and methods ===", flush=True)
    for p in root.rglob("*.smali"):
        if p.name.startswith(("BreakRuntime", "BreakRules")):
            print(f"--- {p} ---", flush=True)
            for i, line in enumerate(p.read_text(encoding="utf-8", errors="ignore").splitlines(), 1):
                if line.startswith((".class", ".super", ".field", ".method")) or re.search(r"strict|block|break|active|enabled|state|start|stop|until|deadline|pref|SharedPreferences|isBreak", line, re.I):
                    print(f"{i}: {line}", flush=True)
    print("=== MainActivity BreakRuntime references ===", flush=True)
    for p in root.rglob("MainActivity.smali"):
        text=p.read_text(encoding="utf-8", errors="ignore")
        if "Lcom/v1f/app/MainActivity;" in text:
            for i,line in enumerate(text.splitlines(),1):
                if "BreakRuntime" in line or "BreakRules" in line:
                    print(f"{p}:{i}: {line}",flush=True)
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
