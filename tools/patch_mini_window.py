#!/usr/bin/env python3
"""Intercept the original app AND website lock entry points after all existing patches."""
from pathlib import Path
import re
import sys

SERVICE = "Lcom/v1f/app/service/AppBlockAccessibilityService;"
COORD = "Lcom/v1f/app/patch/MiniWindowLockCoordinator;"


def replace_method(text, signature, transform):
    start = text.find(signature)
    if start < 0 or text.count(signature) != 1:
        raise RuntimeError("Expected one " + signature)
    end = text.find(".end method", start)
    if end < 0:
        raise RuntimeError("Unterminated " + signature)
    method = text[start:end]
    updated = transform(method)
    if updated == method:
        raise RuntimeError("No change to " + signature)
    return text[:start] + updated + text[end:]


def inject_after_registers(method, hook):
    match = re.search(r"(?m)^    \.(?:locals|registers) \d+\n", method)
    if not match:
        raise RuntimeError("Missing method register declaration")
    return method[:match.end()] + hook + method[match.end():]


def main(root):
    paths = list(root.rglob("com/v1f/app/service/AppBlockAccessibilityService.smali"))
    if len(paths) != 1:
        raise RuntimeError("Expected exactly one AppBlockAccessibilityService")
    path = paths[0]
    text = path.read_text()

    # Must follow GuardBridgeGrace.shouldDelay so the existing 15-second
    # GuardBridge exception is preserved when it is the blocked app.
    def app(method):
        mark = "    :vx_guardbridge_lock_now\n"
        if method.count(mark) != 1:
            raise RuntimeError("GuardBridge grace anchor missing")
        hook = (
            f"    invoke-static {{p0, p1}}, {COORD}->interceptApp(Landroid/accessibilityservice/AccessibilityService;Ljava/lang/String;)Z\n"
            "    move-result v0\n"
            "    if-eqz v0, :vx_mini_original_app_lock\n"
            "    return-void\n"
            "    :vx_mini_original_app_lock\n"
        )
        return method.replace(mark, mark + hook, 1)

    text = replace_method(
        text, ".method private final showApplicationLock(Ljava/lang/String;)V\n", app)

    def website(method):
        # The browser is p2. Do NOT launch the blocked domain as a URL.
        match = re.search(r'(?m)^\s*\.param p2, "browserPackage"[^\n]*\n', method)
        if not match or ".locals 2" not in method:
            raise RuntimeError("Unexpected website lock signature or register budget")
        hook = (
            f"    invoke-static {{p0, p1, p2}}, {COORD}->interceptWebsite(Landroid/accessibilityservice/AccessibilityService;Ljava/lang/String;Ljava/lang/String;)Z\n"
            "    move-result v0\n"
            "    if-eqz v0, :vx_mini_original_website_lock\n"
            "    return-void\n"
            "    :vx_mini_original_website_lock\n"
        )
        return method[:match.end()] + hook + method[match.end():]

    text = replace_method(
        text, ".method private final showWebsiteLock(Ljava/lang/String;Ljava/lang/String;)V\n", website)

    def connected(method):
        anchor = ("    invoke-super {p0}, Landroid/accessibilityservice/AccessibilityService;"
                  "->onServiceConnected()V\n")
        if method.count(anchor) != 1:
            raise RuntimeError("Unexpected service connection callback")
        hook = (
            f"    invoke-static {{p0}}, {COORD}->attach(Landroid/accessibilityservice/AccessibilityService;)V\n")
        return method.replace(anchor, anchor + hook, 1)

    text = replace_method(text, ".method protected onServiceConnected()V\n", connected)
    def stopped(method):
        hook = f"    invoke-static {{}}, {COORD}->detach()V\n"
        return inject_after_registers(method, hook)

    text = replace_method(text, ".method public onDestroy()V\n", stopped)
    text = replace_method(text, ".method public final patchPause()V\n", stopped)
    path.write_text(text)
    print("Installed Mini-window guard for blocked apps and originating browsers")


if __name__ == "__main__":
    main(Path(sys.argv[1]))
