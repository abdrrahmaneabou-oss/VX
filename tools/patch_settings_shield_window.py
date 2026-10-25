#!/usr/bin/env python3
"""Keep the accessibility overlay window but replace its artwork with an opaque timed lock."""
from pathlib import Path
import re
import sys

root = Path(sys.argv[1])
matches = list(root.rglob("MeteorShieldOverlay.smali"))
if len(matches) != 1:
    raise RuntimeError("Expected one MeteorShieldOverlay")
path = matches[0]
source = path.read_text()
needle = "    invoke-interface {v6, v7, v8}, Landroid/view/WindowManager;->addView(Landroid/view/View;Landroid/view/ViewGroup$LayoutParams;)V"
if source.count(needle) != 1:
    raise RuntimeError("Unexpected shield addView bytecode")
hook = "    invoke-static {v0, v2}, Lcom/v1f/app/patch/SettingsShieldWindow;->configure(Landroid/view/View;Landroid/view/WindowManager$LayoutParams;)V\n\n"
path.write_text(source.replace(needle, hook + needle, 1))

# Override the original gradient/meteor drawing only for this shield view.
draw_method = re.compile(r"(?ms)^(?P<signature>\.method[^\n]*\bonDraw\(Landroid/graphics/Canvas;\)V)\n.*?^\.end method$")
views = []
for candidate in root.rglob("*MeteorShieldView*.smali"):
    content = candidate.read_text()
    match = draw_method.search(content)
    if match:
        views.append((candidate, content, match))
if len(views) != 1:
    raise RuntimeError(f"Expected one MeteorShieldView.onDraw(Canvas), found {len(views)}")
view_path, content, match = views[0]
replacement = (match.group("signature") + "\n"
               "    .locals 0\n"
               "    invoke-static/range {p0 .. p1}, Lcom/v1f/app/patch/SettingsShieldWindow;->draw(Landroid/view/View;Landroid/graphics/Canvas;)V\n"
               "    return-void\n"
               ".end method")
view_path.write_text(content[:match.start()] + replacement + content[match.end():])
print("Opaque black Settings shield with red lock and ascending seconds")
