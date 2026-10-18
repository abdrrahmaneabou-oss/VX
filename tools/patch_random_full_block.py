#!/usr/bin/env python3
"""Intercept user-app foreground changes during hourly random full-block windows."""
from pathlib import Path
import re
import sys

root = Path(sys.argv[1])
matches = list(root.rglob("com/v1f/app/service/AppBlockAccessibilityService.smali"))
if len(matches) != 1:
    raise RuntimeError(f"Expected one accessibility service, got {matches}")
path = matches[0]
text = path.read_text()
pattern = re.compile(r'(?ms)(^\.method public onAccessibilityEvent\(Landroid/view/accessibility/AccessibilityEvent;\)V\n\s*\.locals 3\n\s*\.param p1, "event"[^\n]*\n)')
match = pattern.search(text)
if not match:
    raise RuntimeError("Unexpected accessibility service event method")
hook = """
    # Random full-app blocking runs before the original selective protection engine.
    invoke-static {p0, p1}, Lcom/v1f/app/patch/RandomFullBlock;->shouldBlock(Landroid/accessibilityservice/AccessibilityService;Landroid/view/accessibility/AccessibilityEvent;)Z

    move-result v0

    if-eqz v0, :vx_random_continue

    invoke-virtual {p1}, Landroid/view/accessibility/AccessibilityEvent;->getPackageName()Ljava/lang/CharSequence;

    move-result-object v0

    if-eqz v0, :vx_random_continue

    invoke-interface {v0}, Ljava/lang/CharSequence;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-direct {p0, v0}, Lcom/v1f/app/service/AppBlockAccessibilityService;->showApplicationLock(Ljava/lang/String;)V

    return-void

    :vx_random_continue
"""
if ":vx_random_continue" in text:
    raise RuntimeError("Already patched")
text = text[:match.end()] + hook + text[match.end():]
attach = "    invoke-static {p0}, Lcom/v1f/app/patch/BreakRuntime;->attach(Lcom/v1f/app/service/AppBlockAccessibilityService;)V"
detach = "    invoke-static {p0}, Lcom/v1f/app/patch/BreakRuntime;->detach(Lcom/v1f/app/service/AppBlockAccessibilityService;)V"
if text.count(attach) != 1 or text.count(detach) != 1:
    raise RuntimeError("Could not identify existing accessibility service lifecycle hooks")
text = text.replace(attach, attach + "\n\n    invoke-static {p0}, Lcom/v1f/app/patch/RandomFullBlock;->attach(Landroid/accessibilityservice/AccessibilityService;)V")
text = text.replace(detach, detach + "\n\n    invoke-static {p0}, Lcom/v1f/app/patch/RandomFullBlock;->detach(Landroid/accessibilityservice/AccessibilityService;)V")
path.write_text(text)
print("Added hourly random full-app protection hook to accessibility service")
