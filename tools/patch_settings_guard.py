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

    before_array = body[:new_array_match.start()]
    start_matches = list(re.finditer(
        r"(?m)^\s*const(?:/4|/16)? v0, 0x[0-9a-fA-F]+\s*$",
        before_array,
    ))
    if not start_matches:
        raise RuntimeError("Could not locate array-size const instruction")
    block_start = start_matches[-1].start()

    # Find the field assignment that ultimately stores the Set. We do not
    # depend on the exact Kotlin helper used (setOf, toSet, SetsKt variants).
    blocked_field = re.search(
        r"(?m)^\s*sput-object\s+v\d+,\s*" + re.escape(TARGET_CLASS) +
        r"->blockedLabels:Ljava/util/Set;\s*$",
        body[new_array_match.end():],
    )
    if not blocked_field:
        raise RuntimeError("Could not locate blockedLabels field assignment")

    field_start = new_array_match.end() + blocked_field.start()
    population_region = body[new_array_match.end():field_start]
    aputs = list(re.finditer(r"(?m)^\s*aput-object\s+[^\n]+$", population_region))
    if not aputs:
        raise RuntimeError("Could not locate array population instructions")

    # Replace only the array construction/population. Preserve the original
    # Kotlin conversion instructions between the last aput-object and sput-object.
    block_end = new_array_match.end() + aputs[-1].end()

    patched_body = body[:block_start] + build_array_block() + body[block_end:]
    patched_method = ".method static constructor <clinit>()V\n" + patched_body + ".end method"
    patched_text = text[:method_match.start()] + patched_method + text[method_match.end():]

    for removed in ("Private DNS", "Reset network settings", "Rest app preferences"):
        if f'const-string v2, "{removed}"' in patched_method:
            raise RuntimeError(f"Removed label still present in guarded array: {removed}")
    for required in LABELS:
        if f'const-string v2, "{smali_quote(required)}"' not in patched_method:
            raise RuntimeError(f"Required label missing: {required}")

    path.write_text(patched_text, encoding="utf-8")
    print(f"Patched {path}")
    print(f"Blocked label count: {len(LABELS)}")
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
