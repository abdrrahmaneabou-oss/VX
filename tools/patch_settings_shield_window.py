#!/usr/bin/env python3
"""Configure the original accessibility shield without changing its artwork."""
from pathlib import Path
import sys

root = Path(sys.argv[1])
matches = list(root.rglob("MeteorShieldOverlay.smali"))
if len(matches) != 1:
    raise RuntimeError("Expected one MeteorShieldOverlay")
path = matches[0]
text = path.read_text()
needle = "    invoke-interface {v6, v7, v8}, Landroid/view/WindowManager;->addView(Landroid/view/View;Landroid/view/ViewGroup$LayoutParams;)V"
if text.count(needle) != 1:
    raise RuntimeError("Unexpected shield addView bytecode")
hook = "    invoke-static {v0, v2}, Lcom/v1f/app/patch/SettingsShieldWindow;->configure(Landroid/view/View;Landroid/view/WindowManager$LayoutParams;)V\n\n"
path.write_text(text.replace(needle, hook + needle))
print("Configured 3x shield window, 75% transparency and touch interception")
