#!/usr/bin/env python3
"""Suspend every protection entry point during an active BreakRuntime break.

The original V2F service, its deferred lock launcher, Settings recovery, and
LockActivity remain wired; the pause barrier only short-circuits their blocking
behavior while a started session is temporarily allowing all apps.
"""
from pathlib import Path
import re
import sys

COORD = "Lcom/v1f/app/patch/BreakPauseController;"


def method_replace(source, signature, transformer):
    if source.count(signature) != 1:
        raise RuntimeError(f"Expected one method {signature}, got {source.count(signature)}")
    start = source.index(signature)
    end = source.index(".end method", start)
    current = source[start:end]
    updated = transformer(current)
    if updated == current:
        raise RuntimeError("Patch did not change " + signature)
    return source[:start] + updated + source[end:]


def entry_guard(method, label):
    m = re.search(r"(?m)^    \.(?:locals|registers) \d+\n", method)
    if not m:
        raise RuntimeError("Missing register declaration in " + label)
    check = (
        f"    invoke-static {{}}, {COORD}->isBreakActive()Z\n"
        "    move-result v0\n"
        f"    if-eqz v0, :vx_break_continue_{label}\n"
        "    return-void\n"
        f"    :vx_break_continue_{label}\n"
    )
    return method[:m.end()] + check + method[m.end():]


def add_attach(method):
    anchor = (
        "    invoke-super {p0}, Landroid/accessibilityservice/AccessibilityService;"
        "->onServiceConnected()V\n"
    )
    if method.count(anchor) != 1:
        raise RuntimeError("Unable to attach break state poll")
    return method.replace(anchor,
        anchor + f"    invoke-static {{p0}}, {COORD}->attach(Landroid/accessibilityservice/AccessibilityService;)V\n",1)


def add_detach(method):
    m = re.search(r"(?m)^    \.(?:locals|registers) \d+\n", method)
    if not m:
        raise RuntimeError("Cannot locate service lifecycle registers")
    return method[:m.end()] + (
        f"    invoke-static {{}}, {COORD}->detach()V\n"
    ) + method[m.end():]


def intercept_event(method):
    m = re.search(r"(?m)^    \.(?:locals|registers) \d+\n", method)
    if not m:
        raise RuntimeError("Cannot locate event registers")
    guard = (
        f"    invoke-static {{p0}}, {COORD}->suppressBlockEvent(Landroid/accessibilityservice/AccessibilityService;)Z\n"
        "    move-result v0\n"
        "    if-eqz v0, :vx_break_dispatch_event\n"
        "    return-void\n"
        "    :vx_break_dispatch_event\n"
    )
    return method[:m.end()] + guard + method[m.end():]


def hook_lock_activity(method):
    at = method.rfind("    return-void")
    if at < 0:
        raise RuntimeError("LockActivity method has no return-void")
    code = (f"    invoke-static {{p0}}, {COORD}->watchLock(Landroid/app/Activity;)V\n")
    return method[:at] + code + method[at:]


def main(root):
    services = list(root.rglob("com/v1f/app/service/AppBlockAccessibilityService.smali"))
    if len(services) != 1:
        raise RuntimeError("Expected exactly one accessibility service")
    path = services[0]
    source = path.read_text()

    source = method_replace(
        source,
        ".method public onAccessibilityEvent(Landroid/view/accessibility/AccessibilityEvent;)V\n",
        intercept_event)
    source = method_replace(
        source, ".method private final showApplicationLock(Ljava/lang/String;)V\n",
        lambda method: entry_guard(method, "app"))
    source = method_replace(
        source, ".method private final showWebsiteLock(Ljava/lang/String;Ljava/lang/String;)V\n",
        lambda method: entry_guard(method, "site"))
    source = method_replace(
        source,
        ".method private static final showLockScreen$lambda$5("
        "Lcom/v1f/app/service/AppBlockAccessibilityService;Ljava/lang/String;Ljava/lang/String;)V\n",
        lambda method: entry_guard(method, "deferred_lock"))
    source = method_replace(
        source, ".method protected onServiceConnected()V\n", add_attach)
    source = method_replace(source, ".method public onDestroy()V\n", add_detach)
    source = method_replace(source, ".method public final patchPause()V\n", add_detach)
    path.write_text(source)

    activities = [p for p in root.rglob("LockActivity.smali")
                  if ".class public final Lcom/v1f/app/LockActivity;" in p.read_text(errors="replace")]
    if len(activities) != 1:
        raise RuntimeError("Expected one original LockActivity")
    path = activities[0]
    source = path.read_text()
    source = method_replace(
        source, ".method protected onCreate(Landroid/os/Bundle;)V\n", hook_lock_activity)
    source = method_replace(
        source, ".method protected onNewIntent(Landroid/content/Intent;)V\n",
        hook_lock_activity)
    if ".method protected onResume()V" in source:
        raise RuntimeError("Unexpected LockActivity onResume override")
    source += (
        "\n.method protected onResume()V\n"
        "    .locals 0\n"
        "    invoke-super {p0}, Landroidx/activity/ComponentActivity;->onResume()V\n"
        f"    invoke-static {{p0}}, {COORD}->watchLock(Landroid/app/Activity;)V\n"
        "    return-void\n"
        ".end method\n"
    )
    path.write_text(source)
    print("Full scheduled-break barrier installed: app/site/deferred/Settings/LockActivity")


if __name__ == "__main__":
    main(Path(sys.argv[1]))
