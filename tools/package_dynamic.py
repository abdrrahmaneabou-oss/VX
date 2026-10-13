#!/usr/bin/env python3
from pathlib import Path
import sys
import zipfile


def main() -> int:
    if len(sys.argv) != 3:
        print("usage: package_dynamic.py <apk> <dex>", file=sys.stderr)
        return 2
    apk = Path(sys.argv[1])
    dex = Path(sys.argv[2])
    if not apk.is_file() or not dex.is_file():
        raise SystemExit("missing APK or supplemental classes file")
    with zipfile.ZipFile(apk, "a", compression=zipfile.ZIP_DEFLATED) as zf:
        if "classes15.dex" in zf.namelist():
            raise SystemExit("classes15.dex already exists")
        zf.write(dex, "classes15.dex")
    with zipfile.ZipFile(apk, "r") as zf:
        if "classes15.dex" not in zf.namelist():
            raise SystemExit("supplemental classes were not packaged")
    print("Supplemental classes packaged")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
