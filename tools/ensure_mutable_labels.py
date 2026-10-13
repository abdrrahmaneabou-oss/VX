#!/usr/bin/env python3
from pathlib import Path
import re
import sys

TARGET = "Lcom/v1f/app/protection/modules/settings/SettingsClickGuardConfig;->blockedLabels:Ljava/util/Set;"


def patch(path: Path) -> None:
    text = path.read_text(encoding="utf-8")
    if "Ljava/util/LinkedHashSet;-><init>(Ljava/util/Collection;)V" in text:
        print("blockedLabels already mutable")
        return

    m = re.search(r"(?m)^(?P<indent>\s*)sput-object\s+(?P<reg>v\d+),\s*" + re.escape(TARGET) + r"\s*$", text)
    if not m:
        raise RuntimeError("blockedLabels assignment not found")

    src = m.group("reg")
    temp = "v1" if src != "v1" else "v2"
    indent = m.group("indent")
    replacement = (
        f"{indent}new-instance {temp}, Ljava/util/LinkedHashSet;\n\n"
        f"{indent}invoke-direct {{{temp}, {src}}}, Ljava/util/LinkedHashSet;-><init>(Ljava/util/Collection;)V\n\n"
        f"{indent}sput-object {temp}, {TARGET}"
    )
    text = text[:m.start()] + replacement + text[m.end():]
    path.write_text(text, encoding="utf-8")
    print(f"Made blockedLabels mutable in {path}")


def main() -> int:
    if len(sys.argv) != 2:
        print("Usage: ensure_mutable_labels.py <apktool-output-dir>", file=sys.stderr)
        return 2
    root = Path(sys.argv[1])
    matches = list(root.rglob("SettingsClickGuardConfig.smali"))
    if len(matches) != 1:
        raise RuntimeError(f"Expected one SettingsClickGuardConfig.smali, found {len(matches)}")
    patch(matches[0])
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
