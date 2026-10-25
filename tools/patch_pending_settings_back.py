#!/usr/bin/env python3
"""Connect existing Settings escape and accessibility callbacks to the back coordinator."""
from pathlib import Path
import sys

GUARD = "Lcom/v1f/app/protection/actions/SettingsEscapeGuard;"
SERVICE = "Lcom/v1f/app/service/AppBlockAccessibilityService;"
COORD = "Lcom/v1f/app/patch/SettingsBackCoordinator;"

def locate(root, file_name, marker):
    matches = [p for p in root.rglob(file_name) if marker in p.read_text()]
    if len(matches) != 1:
        raise RuntimeError("Expected exactly one " + file_name)
    return matches[0]

def modify_method(text, signature, marker, insertion):
    start = text.index(signature)
    end = text.index(".end method", start)
    method = text[start:end]
    if method.count(marker) != 1:
        raise RuntimeError("Unexpected bytecode layout in " + signature)
    return text[:start] + method.replace(marker, marker + insertion, 1) + text[end:]

def main(root):
    guard = locate(root, "SettingsEscapeGuard.smali", GUARD)
    source = guard.read_text()
    enter = (
        "    iget-object v0, p0, " + GUARD +
        "->service:Landroid/accessibilityservice/AccessibilityService;\n"
        "    iget-object v1, p0, " + GUARD +
        "->overlay:Lcom/v1f/app/protection/overlay/MeteorShieldOverlay;\n"
        "    invoke-static {v0, v1}, " + COORD +
        "->trigger(Landroid/accessibilityservice/AccessibilityService;"
        "Lcom/v1f/app/protection/overlay/MeteorShieldOverlay;)V\n"
        "    return-void\n"
    )
    source = modify_method(source, ".method public final trigger()V",
                           "    :cond_0\n", enter)
    source = modify_method(source, ".method public final close()V",
                           "    .locals 2\n",
                           "    invoke-static {}, " + COORD + "->cancel()V\n")
    guard.write_text(source)

    service = locate(root, "AppBlockAccessibilityService.smali", SERVICE)
    source = service.read_text()
    observe = (
        "    invoke-static/range {p0 .. p1}, " + COORD +
        "->observe(Landroid/accessibilityservice/AccessibilityService;"
        "Landroid/view/accessibility/AccessibilityEvent;)V\n"
    )
    source = modify_method(
        source,
        ".method public onAccessibilityEvent(Landroid/view/accessibility/AccessibilityEvent;)V",
        "    :cond_0\n", observe
    )
    service.write_text(source)
    print("Settings escape and event observer connected")

if __name__ == "__main__":
    main(Path(sys.argv[1]))
