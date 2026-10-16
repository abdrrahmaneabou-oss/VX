#!/usr/bin/env python3
from pathlib import Path
import re
import sys

ACTIVITY = "com.v1f.app.patch.DynamicLabelsActivity"
STORE = "Lcom/v1f/app/patch/DynamicLabelsStore;"
ROBUST_GUARD = "Lcom/v1f/app/patch/RobustSettingsGuard;"
GUARD_CONFIG = "Lcom/v1f/app/protection/modules/settings/SettingsClickGuardConfig;"
DETECTOR = "Lcom/v1f/app/protection/modules/settings/SettingsClickDetector;"


def find_class(root: Path, filename: str, descriptor: str) -> Path:
    matches = [
        p for p in root.rglob(filename)
        if descriptor in p.read_text(encoding="utf-8", errors="ignore")
    ]
    if len(matches) != 1:
        raise RuntimeError(f"Expected one {filename} for {descriptor}, found {len(matches)}")
    return matches[0]


def patch_manifest(root: Path) -> None:
    path = root / "AndroidManifest.xml"
    text = path.read_text(encoding="utf-8")
    if ACTIVITY in text:
        return
    line = (
        '        <activity android:exported="false" '
        'android:name="com.v1f.app.patch.DynamicLabelsActivity" '
        'android:screenOrientation="portrait" '
        'android:theme="@style/Theme.V1F.Duration"/>\n'
    )
    if "</application>" not in text:
        raise RuntimeError("Manifest has no application closing tag")
    text = text.replace("    </application>", line + "    </application>", 1)
    path.write_text(text, encoding="utf-8")


def patch_application(root: Path) -> None:
    matches = list(root.rglob("V1FApplication.smali"))
    if len(matches) != 1:
        raise RuntimeError(f"Expected one V1FApplication.smali, found {len(matches)}")
    path = matches[0]
    text = path.read_text(encoding="utf-8")
    if "BreakIntervalStore;->init" in text:
        return
    method = re.search(r"(?ms)^\.method public onCreate\(\)V\n(?P<body>.*?)^\.end method$", text)
    if not method:
        raise RuntimeError("V1FApplication.onCreate() not found")
    body = method.group("body")
    marker = "    return-void"
    pos = body.rfind(marker)
    if pos < 0:
        raise RuntimeError("No return-void in V1FApplication.onCreate")
    injection = ("    invoke-static {p0}, Lcom/v1f/app/patch/DynamicLabelsStore;->init(Landroid/content/Context;)V\n\n"
                 "    invoke-static {p0}, Lcom/v1f/app/patch/BreakIntervalStore;->init(Landroid/content/Context;)V\n\n")
    body = body[:pos] + injection + body[pos:]
    replacement = ".method public onCreate()V\n" + body + ".end method"
    text = text[:method.start()] + replacement + text[method.end():]
    path.write_text(text, encoding="utf-8")


def patch_main_activity(root: Path) -> None:
    matches = [
        p for p in root.rglob("MainActivity.smali")
        if "Lcom/v1f/app/MainActivity;" in p.read_text(encoding="utf-8", errors="ignore")
    ]
    if len(matches) != 1:
        raise RuntimeError(f"Expected one MainActivity.smali, found {len(matches)}")
    path = matches[0]
    text = path.read_text(encoding="utf-8")
    if "DynamicLabelsEntry;->attach" in text:
        return
    needle = "    invoke-static {p0}, Lcom/v1f/app/patch/BreakRuntime;->attachStatus(Landroid/app/Activity;)V"
    if needle not in text:
        raise RuntimeError("BreakRuntime.attachStatus hook not found in MainActivity")
    injection = "    invoke-static {p0}, Lcom/v1f/app/patch/DynamicLabelsEntry;->attach(Landroid/app/Activity;)V\n\n"
    text = text.replace(needle, injection + needle, 1)
    path.write_text(text, encoding="utf-8")


def patch_guard_config(root: Path) -> None:
    path = find_class(root, "SettingsClickGuardConfig.smali", GUARD_CONFIG)
    text = path.read_text(encoding="utf-8")
    marker = f"{STORE}->snapshot()Ljava/util/Set;"
    if marker in text:
        return
    method = re.search(
        r"(?ms)^(?P<head>\.method[^\n]*\sgetBlockedLabels\(\)Ljava/util/Set;\n)(?P<body>.*?)^\.end method$",
        text,
    )
    if not method:
        raise RuntimeError("SettingsClickGuardConfig.getBlockedLabels() not found")
    replacement = (
        method.group("head")
        + "    .locals 1\n\n"
        + "    invoke-static {}, Lcom/v1f/app/patch/DynamicLabelsStore;->snapshot()Ljava/util/Set;\n\n"
        + "    move-result-object v0\n\n"
        + "    return-object v0\n"
        + ".end method"
    )
    text = text[:method.start()] + replacement + text[method.end():]
    path.write_text(text, encoding="utf-8")


def patch_detector(root: Path) -> None:
    path = find_class(root, "SettingsClickDetector.smali", DETECTOR)
    text = path.read_text(encoding="utf-8")
    marker = f"{ROBUST_GUARD}->detect"
    if marker in text:
        return
    method = re.search(
        r"(?ms)^\.method[^\n]*\sdetect\(Landroid/view/accessibility/AccessibilityEvent;Ljava/lang/String;\)Ljava/lang/String;\n(?P<body>.*?)^\.end method$",
        text,
    )
    if not method:
        raise RuntimeError("SettingsClickDetector.detect(event, packageName) not found")
    body = method.group("body")
    first_instruction = re.search(r"(?m)^    (?![.#:])[a-z][^\n]*$", body)
    if not first_instruction:
        raise RuntimeError("No instruction found in SettingsClickDetector.detect")

    # p1/p2 map to high physical registers in this Kotlin-generated method.
    # Normal invoke-static uses 4-bit register operands (v0..v15), so use the
    # range form to safely pass adjacent parameter registers regardless of the
    # method's local-register count.
    injection = (
        "    invoke-static/range {p1 .. p2}, Lcom/v1f/app/patch/RobustSettingsGuard;->detect("
        "Landroid/view/accessibility/AccessibilityEvent;Ljava/lang/String;)Ljava/lang/String;\n\n"
        "    move-result-object v0\n\n"
        "    if-eqz v0, :v2f_original_settings_detector\n\n"
        "    return-object v0\n\n"
        ":v2f_original_settings_detector\n"
    )
    body = body[:first_instruction.start()] + injection + body[first_instruction.start():]
    text = text[:method.start("body")] + body + text[method.end("body"):]
    path.write_text(text, encoding="utf-8")


def main() -> int:
    if len(sys.argv) != 2:
        print("Usage: patch_dynamic_labels.py <apktool-output-dir>", file=sys.stderr)
        return 2
    root = Path(sys.argv[1])
    patch_manifest(root)
    patch_application(root)
    patch_main_activity(root)
    patch_guard_config(root)
    patch_detector(root)
    print("Dynamic label manager + robust Settings guard hooks installed")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
