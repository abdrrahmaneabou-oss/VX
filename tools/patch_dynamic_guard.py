#!/usr/bin/env python3
from pathlib import Path
import re
import sys

CONFIG_CLASS = "Lcom/v1f/app/protection/modules/settings/SettingsClickGuardConfig;"
DETECTOR_CLASS = "Lcom/v1f/app/protection/modules/settings/SettingsClickDetector;"
STORE_CLASS = "Lcom/v1f/app/patch/SettingsSessionGate;"
ROBUST_CLASS = "Lcom/v1f/app/patch/RobustSettingsGuard;"


def find_one(root: Path, name: str, marker: str) -> Path:
    matches = [
        p for p in root.rglob(name)
        if marker in p.read_text(encoding="utf-8", errors="ignore")
    ]
    if len(matches) != 1:
        raise RuntimeError(f"Expected exactly one {name}, found {len(matches)}")
    return matches[0]


def patch_config_getter(root: Path) -> None:
    path = find_one(root, "SettingsClickGuardConfig.smali", CONFIG_CLASS)
    text = path.read_text(encoding="utf-8")
    method = re.search(
        r"(?ms)^\.method public final getBlockedLabels\(\)Ljava/util/Set;\n.*?^\.end method$",
        text,
    )
    if not method:
        raise RuntimeError("SettingsClickGuardConfig.getBlockedLabels() not found")
    replacement = (
        ".method public final getBlockedLabels()Ljava/util/Set;\n"
        "    .locals 1\n\n"
        f"    invoke-static {{}}, {STORE_CLASS}->blockedLabelsSnapshot()Ljava/util/Set;\n\n"
        "    move-result-object v0\n\n"
        "    return-object v0\n"
        ".end method"
    )
    text = text[:method.start()] + replacement + text[method.end():]
    path.write_text(text, encoding="utf-8")
    print(f"Patched dynamic blocked-label getter in {path}")


def patch_detector(root: Path) -> None:
    path = find_one(root, "SettingsClickDetector.smali", DETECTOR_CLASS)
    text = path.read_text(encoding="utf-8")
    method = re.search(
        r"(?ms)^\.method public final detect\(Landroid/view/accessibility/AccessibilityEvent;Ljava/lang/String;\)Ljava/lang/String;\n(?P<body>.*?)^\.end method$",
        text,
    )
    if not method:
        raise RuntimeError("SettingsClickDetector.detect(event, packageName) not found")

    body = method.group("body")
    if "RobustSettingsGuard;->detect" in body:
        print("Robust Settings detector already hooked")
        return

    locals_match = re.search(r"(?m)^\s*\.locals\s+(\d+)\s*$", body)
    if not locals_match:
        raise RuntimeError("SettingsClickDetector.detect does not use .locals")

    # Keep the original local count so parameter registers do not shift. p1/p2
    # are high physical registers in this Kotlin-generated method, so the range
    # invoke form is required (normal invoke-static only encodes v0..v15).
    temp = "v0"
    insert_at = body.find("\n", locals_match.end()) + 1
    if insert_at <= 0:
        insert_at = locals_match.end()
    hook = (
        "\n"
        "    invoke-static {}, Lcom/v1f/app/patch/SettingsSessionGate;->isActive()Z\n"
        "    move-result v0\n"
        "    if-nez v0, :vx_guard_session_active\n"
        "    const/4 v0, 0x0\n"
        "    return-object v0\n"
        "    :vx_guard_session_active\n"
        f"    invoke-static/range {{p1 .. p2}}, {ROBUST_CLASS}->detect(Landroid/view/accessibility/AccessibilityEvent;Ljava/lang/String;)Ljava/lang/String;\n\n"
        f"    move-result-object {temp}\n\n"
        f"    if-eqz {temp}, :vx_original_detector\n\n"
        f"    return-object {temp}\n\n"
        "    :vx_original_detector\n"
    )
    body = body[:insert_at] + hook + body[insert_at:]

    replacement = (
        ".method public final detect(Landroid/view/accessibility/AccessibilityEvent;Ljava/lang/String;)Ljava/lang/String;\n"
        + body
        + ".end method"
    )
    text = text[:method.start()] + replacement + text[method.end():]
    path.write_text(text, encoding="utf-8")
    print(f"Hooked robust Settings detector in {path}")


def main() -> int:
    if len(sys.argv) != 2:
        print("Usage: patch_dynamic_guard.py <apktool-output-dir>", file=sys.stderr)
        return 2
    root = Path(sys.argv[1])
    patch_config_getter(root)
    patch_detector(root)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
