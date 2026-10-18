#!/usr/bin/env python3
"""Patch the run #44 LockActivity in-place: each blocked intent has a 3s gate."""
from pathlib import Path
import re
import sys

CLASS = "Lcom/v1f/app/LockActivity;"
FIELD = "Lcom/v1f/app/LockActivity;->vxUnlockAt:J"
DELAY = 3000

def method_replace(source, signature, edit):
    pat = re.compile(r"(?ms)^\.method " + re.escape(signature) + r"\n.*?^\.end method$")
    matches = list(pat.finditer(source))
    if len(matches) != 1:
        raise RuntimeError("Expected one method: " + signature)
    m = matches[0]
    return source[:m.start()] + edit(m.group()) + source[m.end():]

def insert_before_return(method, instructions):
    idx = method.rfind("    return-void")
    if idx < 0:
        raise RuntimeError("Missing return-void")
    return method[:idx] + instructions + method[idx:]

def reset_timer():
    return (
        "    invoke-static {}, Landroid/os/SystemClock;->elapsedRealtime()J\n"
        "    move-result-wide v0\n"
        "    const-wide/16 v2, 0xbb8\n"
        "    add-long/2addr v0, v2\n"
        f"    iput-wide v0, p0, {FIELD}\n\n"
    )

def main():
    root = Path(sys.argv[1])
    matches = [p for p in root.rglob("LockActivity.smali") if ".class public final " + CLASS in p.read_text(errors="ignore")]
    if len(matches) != 1:
        raise RuntimeError(f"Expected exactly one LockActivity, got {len(matches)}")
    path = matches[0]
    text = path.read_text()
    if "vxUnlockAt:J" in text:
        raise RuntimeError("Delay patch already applied")
    text = text.replace("# direct methods", f".field private vxUnlockAt:J\n\n# direct methods", 1)

    def patch_oncreate(m):
        # Existing method has four locals, enough for the elapsed-time long pairs.
        if ".locals 4" not in m:
            raise RuntimeError("Unexpected onCreate locals")
        return insert_before_return(m, reset_timer())
    text = method_replace(text, "protected onCreate(Landroid/os/Bundle;)V", patch_oncreate)

    def patch_newintent(m):
        m = m.replace("    .locals 1", "    .locals 4", 1)
        return insert_before_return(m, reset_timer())
    text = method_replace(text, "protected onNewIntent(Landroid/content/Intent;)V", patch_newintent)

    def patch_home(m):
        m = m.replace("    .locals 4", "    .locals 4", 1)
        needle = "    .line 52\n"
        guard = (
            f"    iget-wide v0, p0, {FIELD}\n"
            "    invoke-static {}, Landroid/os/SystemClock;->elapsedRealtime()J\n"
            "    move-result-wide v2\n"
            "    cmp-long v0, v2, v0\n"
            "    if-gez v0, :vx_can_go_home\n"
            "    return-void\n"
            "    :vx_can_go_home\n\n"
        )
        if needle not in m:
            raise RuntimeError("Unexpected returnToLauncher method")
        return m.replace(needle, guard + needle, 1)
    text = method_replace(text, "private final returnToLauncher()V", patch_home)

    # The Compose button calls returnToLauncher; the Activity back key is gated too.
    extra = (
        "\n.method public onBackPressed()V\n"
        "    .locals 0\n"
        f"    invoke-direct {{p0}}, {CLASS}->returnToLauncher()V\n"
        "    return-void\n"
        ".end method\n"
    )
    text += extra
    path.write_text(text)
    print(f"Applied 3000ms intent-scoped exit gate: {path}")

if __name__ == "__main__":
    main()
