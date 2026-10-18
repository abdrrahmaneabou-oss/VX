#!/usr/bin/env python3
"""Delay GuardBridge's actual black lock screen, never any other app."""
from pathlib import Path
import re
import sys

root = Path(sys.argv[1])
paths = list(root.rglob("com/v1f/app/service/AppBlockAccessibilityService.smali"))
if len(paths) != 1:
    raise RuntimeError("Expected one accessibility service")
path = paths[0]
s = path.read_text()
event = re.search(r'(?m)^\.method public onAccessibilityEvent\(Landroid/view/accessibility/AccessibilityEvent;\)V\n\s*\.locals 3\n\s*\.param p1, "event"[^\n]*\n', s)
if not event:
    raise RuntimeError("Unexpected onAccessibilityEvent")
s = s[:event.end()] + """
    invoke-static {p0, p1}, Lcom/v1f/app/patch/GuardBridgeGrace;->onEvent(Landroid/accessibilityservice/AccessibilityService;Landroid/view/accessibility/AccessibilityEvent;)V
""" + s[event.end():]
lock = re.search(r'(?m)^\.method private final showApplicationLock\(Ljava/lang/String;\)V\n\s*\.locals 7\n\s*\.param p1, "packageName"[^\n]*\n', s)
if not lock:
    raise RuntimeError("Unexpected showApplicationLock")
s = s[:lock.end()] + """
    invoke-static {p0, p1}, Lcom/v1f/app/patch/GuardBridgeGrace;->shouldDelay(Landroid/accessibilityservice/AccessibilityService;Ljava/lang/String;)Z
    move-result v0
    if-eqz v0, :vx_guardbridge_lock_now
    return-void
    :vx_guardbridge_lock_now
""" + s[lock.end():]
detach = "    invoke-static {p0}, Lcom/v1f/app/patch/RandomFullBlock;->detach(Landroid/accessibilityservice/AccessibilityService;)V"
if s.count(detach) != 1:
    raise RuntimeError("Cannot find accessibility lifecycle detach")
s = s.replace(detach, detach + "\n\n    invoke-static {p0}, Lcom/v1f/app/patch/GuardBridgeGrace;->detach(Landroid/accessibilityservice/AccessibilityService;)V")
path.write_text(s)
print("GuardBridge grace hooked at event tracking and shared lock entry")
