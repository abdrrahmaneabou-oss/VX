#!/usr/bin/env python3
"""Five BACK attempts at 250ms intervals before the original app/site lock.

Only hook native application/website lock methods; leave Settings fully alone.
The original LockActivity and website/app detection engines remain unchanged.
"""
from pathlib import Path
import re
import sys

COORD = "Lcom/v1f/app/patch/BlockedBackCoordinator;"


def replace_method(text, signature, transformer):
    if text.count(signature) != 1:
        raise RuntimeError("Expected exactly one " + signature)
    start = text.index(signature)
    end = text.index(".end method", start)
    previous = text[start:end]
    updated = transformer(previous)
    if updated == previous:
        raise RuntimeError("No change made for " + signature)
    return text[:start] + updated + text[end:]


def insert_after_locals(method, extra):
    match = re.search(r"(?m)^    \.(?:locals|registers) \d+\n", method)
    if not match:
        raise RuntimeError("Missing method register declaration")
    return method[:match.end()] + extra + method[match.end():]


def main(root):
    paths = list(root.rglob("com/v1f/app/service/AppBlockAccessibilityService.smali"))
    if len(paths) != 1:
        raise RuntimeError("Expected exactly one AppBlockAccessibilityService.smali")
    path = paths[0]
    content = path.read_text()

    def app(method):
        # GuardBridgeGrace is a pre-existing exception: it has to run FIRST.
        anchor = "    :vx_guardbridge_lock_now\n"
        if method.count(anchor) != 1:
            raise RuntimeError("GuardBridge grace hook unexpectedly absent")
        patch = (
            f"    invoke-static {{p0, p1}}, {COORD}->interceptApp(Landroid/accessibilityservice/AccessibilityService;Ljava/lang/String;)Z\n"
            "    move-result v0\n"
            "    if-eqz v0, :vx_five_back_app_stock\n"
            "    return-void\n"
            "    :vx_five_back_app_stock\n"
        )
        return method.replace(anchor, anchor + patch, 1)

    content = replace_method(
        content, ".method private final showApplicationLock(Ljava/lang/String;)V\n", app)

    def website(method):
        anchor = re.search(r'(?m)^\s*\.param p2, "browserPackage"[^\n]*\n', method)
        if not anchor or ".locals 2" not in method:
            raise RuntimeError("Unexpected website lock parameters")
        patch = (
            f"    invoke-static {{p0, p1, p2}}, {COORD}->interceptWebsite(Landroid/accessibilityservice/AccessibilityService;Ljava/lang/String;Ljava/lang/String;)Z\n"
            "    move-result v0\n"
            "    if-eqz v0, :vx_five_back_site_stock\n"
            "    return-void\n"
            "    :vx_five_back_site_stock\n"
        )
        return method[:anchor.end()] + patch + method[anchor.end():]

    content = replace_method(
        content, ".method private final showWebsiteLock(Ljava/lang/String;Ljava/lang/String;)V\n", website)

    detach = f"    invoke-static {{}}, {COORD}->cancel()V\n"
    for signature in (
        ".method public onDestroy()V\n",
        ".method public final patchPause()V\n",
        ".method public onInterrupt()V\n",
    ):
        content = replace_method(content, signature,
                                 lambda method: insert_after_locals(method, detach))

    path.write_text(content)
    print("Five BACKs/250ms before stock application and website locks; Settings untouched")


if __name__ == "__main__":
    main(Path(sys.argv[1]))
