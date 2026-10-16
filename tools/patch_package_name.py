#!/usr/bin/env python3
from pathlib import Path
import re
import sys

OLD_PKG = "com.v1f.app"
NEW_PKG = "com.vx.app"

OLD_DESC = "Lcom/v1f/app/"
NEW_DESC = "Lcom/vx/app/"

def patch_file(path: Path) -> bool:
    try:
        content = path.read_text(encoding="utf-8", errors="ignore")
    except Exception:
        return False

    modified = False
    new_content = content

    if OLD_PKG in new_content or OLD_DESC in new_content:
        new_content = new_content.replace(OLD_DESC, NEW_DESC).replace(OLD_PKG, NEW_PKG)
        modified = True

    if path.name == "AndroidManifest.xml":
        # Target application label specifically
        new_content = re.sub(r'(<application\b[^>]*?\bandroid:label=")[^"]*(")', r'\g<1>Vx\g<2>', new_content)
        modified = True

    if path.name == "strings.xml":
        # Rename app_name string resource if present
        new_content = re.sub(r'<string name="app_name">[^<]*</string>', '<string name="app_name">Vx</string>', new_content)
        modified = True

    if modified:
        path.write_text(new_content, encoding="utf-8")

    return modified

def main() -> int:
    if len(sys.argv) != 2:
        print("Usage: patch_package_name.py <apktool-output-dir>", file=sys.stderr)
        return 2

    root = Path(sys.argv[1])
    if not root.exists():
        print(f"Error: Directory {root} does not exist", file=sys.stderr)
        return 1

    changed_count = 0
    for file_path in root.rglob("*"):
        if file_path.is_file() and file_path.suffix in (".xml", ".smali", ".txt", ".yml"):
            if patch_file(file_path):
                changed_count += 1

    print(f"Package rename ({OLD_PKG} -> {NEW_PKG}) & App rename to Vx applied. Modified {changed_count} files.")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
