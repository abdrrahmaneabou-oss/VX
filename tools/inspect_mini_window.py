#!/usr/bin/env python3
"""Build-time decompiled APK audit for app and website lock interception."""
from pathlib import Path
import re, sys
root=Path(sys.argv[1])
needles=("showApplicationLock","showWebsite","LockActivity","blockedSite","blockedUrl",
         "WebsiteGuard","getPackageName","ACTION_MAIN","Intent;-><init>",
         "putExtra","const-class","startActivity")
for f in root.rglob("*.smali"):
    if f.name not in ("AppBlockAccessibilityService.smali","LockActivity.smali") and not any(k in f.name for k in ("WebsiteGuard","WebsiteBlock","WebsiteDetector","WebsiteLock","ProtectionEngine","LockAction")):
        continue
    t=f.read_text(errors="replace")
    lines=t.splitlines()
    print("\n*** INSPECT MINI: ",f.relative_to(root)," lines=",len(lines))
    current=""
    for i,l in enumerate(lines):
        if l.startswith(".method "):current=l
        if l.startswith(".method ") or l.startswith(".field "):
            print(f"  {i+1}: {l[:220]}")
        if any(n in l for n in needles):
            print(f"  @ {i+1} [method {current[:90]}] => {l[:220]}")
            if any(n in l for n in ("showApplicationLock","showWebsite","LockActivity","startActivity")):
                print("    nearby:", " | ".join(x.strip()[:140] for x in lines[max(0,i-4):min(len(lines),i+6)]))
