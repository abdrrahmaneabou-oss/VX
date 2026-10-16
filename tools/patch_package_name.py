#!/usr/bin/env python3
from pathlib import Path
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
    if OLD_PKG in content or OLD_DESC in content:
        new_content = content.replace(OLD_DESC, NEW_DESC).replace(OLD_PKG, NEW_PKG)
        path.write_text(new_content, encoding="utf-8")
        modified = True
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

    print(f"Package rename applied ({OLD_PKG} -> {NEW_PKG}). Modified {changed_count} files.")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
