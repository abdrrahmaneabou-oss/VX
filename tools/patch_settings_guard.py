#!/usr/bin/env python3
from pathlib import Path
import re
import sys

LABELS = [
    "System Languages",
    "Developer options",
    "Device admin",
    "DEACTIVATE THIS DEVICE ADMIN APP",
    "Uninstall",
    "DEACTIVATE & UNINSTALL",
    "Reset app preferences",
    "Background running",
    "V1F App Guard",
    "V1F Device Protection",
    "SAFEX Accessibility",
    "SAFEX Device Admin",
    "About phone",
    "Build number",
    "Wireless debugging",
    "BlockerHero",
    "Reset options",
    "Add a language",
    "Restore Applications",
]

TARGET_CLASS = "Lcom/v1f/app/protection/modules/settings/SettingsClickGuardConfig;"


def smali_quote(value: str) -> str:
    return value.replace("\\", "\\\\").replace('"', '\\"')


def index_const(index: int) -> str:
    if 0 <= index <= 7:
        return f"    const/4 v1, 0x{index:x}"
    return f"    const/16 v1, 0x{index:x}"


def build_array_block() -> str:
    lines = [
        f"    const/16 v0, 0x{len(LABELS):x}",
        "",
        "    new-array v0, v0, [Ljava/lang/String;",
        "",
    ]
    for index, label in enumerate(LABELS):
        lines.extend([
            index_const(index),
            "",
            f'    const-string v2, "{smali_quote(label)}"',
            "",
            "    aput-object v2, v0, v1",
            "",
        ])
    return "\n".join(lines)


def patch_file(path: Path) -> None:
    text = path.read_text(encoding="utf-8")
    if TARGET_CLASS not in text:
        raise RuntimeError(f"Unexpected class file: {path}")

    method_match = re.search(
        r"(?ms)^\.method static constructor <clinit>\(\)V\n(?P<body>.*?)^\.end method$",
        text,
    )
    if not method_match:
        raise RuntimeError("Could not locate static constructor <clinit>()V")

    body = method_match.group("body")
    new_array_match = re.search(
        r"(?m)^\s*new-array v0, v0, \[Ljava/lang/String;$",
        body,
    )
    if not new_array_match:
        raise RuntimeError("Could not locate blocked-label String array")

    # Start at the const instruction immediately preceding new-array.
    before_array = body[:new_array_match.start()]
    start_match = list(re.finditer(r"(?m)^\s*const(?:/4|/16)? v0, 0x[0-9a-fA-F]+\s*$", before_array))
    if not start_match:
        raise RuntimeError("Could not locate array-size const instruction")
    block_start = start_match[-1].start()

    # Stop immediately before the Kotlin setOf(...) call. The original method
    # then stores the resulting Set in blockedLabels and initializes $stable.
    setof_match = re.search(
        r"(?m)^\s*invoke-static \{v0\}, Lkotlin/collections/SetsKt[^;]*;->setOf\(\[Ljava/lang/Object;\)Ljava/util/Set;\s*$",
        body[new_array_match.end():],
    )
    if not setof_match:
        # Apktool/smali versions can emit SetsKt or SetsKt__SetsKt.
        setof_match = re.search(
            r"(?m)^\s*invoke-static \{v0\}, Lkotlin/collections/[^;]+;->setOf\(\[Ljava/lang/Object;\)Ljava/util/Set;\s*$",
            body[new_array_match.end():],
        )
    if not setof_match:
        raise RuntimeError("Could not locate setOf(...) call after blocked-label array")
    block_end = new_array_match.end() + setof_match.start()

    patched_body = body[:block_start] + build_array_block() + "\n" + body[block_end:]
    patched_method = ".method static constructor <clinit>()V\n" + patched_body + ".end method"
    patched_text = text[:method_match.start()] + patched_method + text[method_match.end():]

    # Safety assertions: removed labels must not remain in the guarded array
    # source, and all requested labels must be present.
    for removed in ("Private DNS", "Reset network settings", "Rest app preferences"):
        if f'const-string v2, "{removed}"' in patched_method:
            raise RuntimeError(f"Removed label still present: {removed}")
    for required in LABELS:
        if f'const-string v2, "{smali_quote(required)}"' not in patched_method:
            raise RuntimeError(f"Required label missing: {required}")

    path.write_text(patched_text, encoding="utf-8")
    print(f"Patched {path}")
    print("Blocked labels:")
    for label in LABELS:
        print(f"  - {label}")


def main() -> int:
    if len(sys.argv) != 2:
        print("Usage: patch_settings_guard.py <apktool-output-dir>", file=sys.stderr)
        return 2
    root = Path(sys.argv[1])
    candidates = [
        p for p in root.rglob("SettingsClickGuardConfig.smali")
        if TARGET_CLASS in p.read_text(encoding="utf-8", errors="ignore")
    ]
    if len(candidates) != 1:
        raise RuntimeError(f"Expected exactly one SettingsClickGuardConfig.smali, found {len(candidates)}")
    patch_file(candidates[0])
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
