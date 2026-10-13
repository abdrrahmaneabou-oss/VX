#!/usr/bin/env python3
from pathlib import Path
import re
import sys

ACTIVITY = "com.v1f.app.patch.DynamicLabelsActivity"


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
    if "DynamicLabelsStore;->init" in text:
        return
    method = re.search(r"(?ms)^\.method public onCreate\(\)V\n(?P<body>.*?)^\.end method$", text)
    if not method:
        raise RuntimeError("V1FApplication.onCreate() not found")
    body = method.group("body")
    marker = "    return-void"
    pos = body.rfind(marker)
    if pos < 0:
        raise RuntimeError("No return-void in V1FApplication.onCreate")
    injection = (
        "    invoke-static {p0}, Lcom/v1f/app/patch/DynamicLabelsStore;->init(Landroid/content/Context;)V\n\n"
    )
    body = body[:pos] + injection + body[pos:]
    replacement = ".method public onCreate()V\n" + body + ".end method"
    text = text[:method.start()] + replacement + text[method.end():]
    path.write_text(text, encoding="utf-8")


def patch_main_activity(root: Path) -> None:
    matches = [p for p in root.rglob("MainActivity.smali") if "Lcom/v1f/app/MainActivity;" in p.read_text(encoding="utf-8", errors="ignore")]
    if len(matches) != 1:
        raise RuntimeError(f"Expected one MainActivity.smali, found {len(matches)}")
    path = matches[0]
    text = path.read_text(encoding="utf-8")
    if "DynamicLabelsEntry;->attach" in text:
        return
    needle = "    invoke-static {p0}, Lcom/v1f/app/patch/BreakRuntime;->attachStatus(Landroid/app/Activity;)V"
    if needle not in text:
        raise RuntimeError("BreakRuntime.attachStatus hook not found in MainActivity")
    injection = (
        "    invoke-static {p0}, Lcom/v1f/app/patch/DynamicLabelsEntry;->attach(Landroid/app/Activity;)V\n\n"
    )
    text = text.replace(needle, injection + needle, 1)
    path.write_text(text, encoding="utf-8")


def main() -> int:
    if len(sys.argv) != 2:
        print("Usage: patch_dynamic_labels.py <apktool-output-dir>", file=sys.stderr)
        return 2
    root = Path(sys.argv[1])
    patch_manifest(root)
    patch_application(root)
    patch_main_activity(root)
    print("Dynamic label manager hooks installed")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
