#!/usr/bin/env python3
from pathlib import Path
import re
import sys

BREAK_RULES = "Lcom/v1f/app/patch/BreakRules;"
STORE = "Lcom/v1f/app/patch/BreakIntervalStore;"
ENTRY = "Lcom/v1f/app/patch/BreakIntervalEntry;"
KNOWN_STATIC_MS = {43_200_000, 86_400_000}  # current 12h build and original 24h build


def find_class(root: Path, filename: str, descriptor: str) -> Path:
    matches = [
        p for p in root.rglob(filename)
        if descriptor in p.read_text(encoding="utf-8", errors="ignore")
    ]
    if len(matches) != 1:
        raise RuntimeError(f"Expected one {filename} for {descriptor}, found {len(matches)}")
    return matches[0]


def parse_literal(raw: str):
    value = raw.strip().rstrip("Ll")
    try:
        return int(value, 0)
    except ValueError:
        return None


def patch_break_rules(root: Path) -> None:
    path = find_class(root, "BreakRules.smali", BREAK_RULES)
    text = path.read_text(encoding="utf-8")
    if f"{STORE}->getIntervalMs()J" in text:
        return

    pattern = re.compile(
        r"(?m)^(?P<indent>\s*)const-wide(?:/16|/32|/high16)?\s+"
        r"(?P<reg>[vp]\d+),\s+(?P<value>-?(?:0x[0-9a-fA-F]+|\d+)[Ll]?)\s*$"
    )

    replacements = 0

    def replace(match: re.Match) -> str:
        nonlocal replacements
        literal = parse_literal(match.group("value"))
        if literal not in KNOWN_STATIC_MS:
            return match.group(0)
        replacements += 1
        indent = match.group("indent")
        reg = match.group("reg")
        return (
            f"{indent}invoke-static {{}}, {STORE}->getIntervalMs()J\n\n"
            f"{indent}move-result-wide {reg}"
        )

    patched = pattern.sub(replace, text)
    if replacements != 2:
        found = []
        for token in ("0x2932e00", "43200000", "0x5265c00", "86400000"):
            if token.lower() in text.lower():
                found.append(token)
        raise RuntimeError(
            f"Expected exactly two static 12h/24h const-wide references in BreakRules, "
            f"patched {replacements}; visible tokens={found}"
        )

    path.write_text(patched, encoding="utf-8")
    print(f"Dynamic interval hooked in {path}; replaced {replacements} static references")


def patch_application(root: Path) -> None:
    path = find_class(root, "V1FApplication.smali", "Lcom/v1f/app/V1FApplication;")
    text = path.read_text(encoding="utf-8")
    marker = f"{STORE}->init(Landroid/content/Context;)V"
    if marker in text:
        return

    method = re.search(r"(?ms)^\.method public onCreate\(\)V\n(?P<body>.*?)^\.end method$", text)
    if not method:
        raise RuntimeError("V1FApplication.onCreate() not found")
    body = method.group("body")
    pos = body.rfind("    return-void")
    if pos < 0:
        raise RuntimeError("No return-void in V1FApplication.onCreate")
    injection = f"    invoke-static {{p0}}, {STORE}->init(Landroid/content/Context;)V\n\n"
    body = body[:pos] + injection + body[pos:]
    replacement = ".method public onCreate()V\n" + body + ".end method"
    text = text[:method.start()] + replacement + text[method.end():]
    path.write_text(text, encoding="utf-8")


def patch_main_activity(root: Path) -> None:
    path = find_class(root, "MainActivity.smali", "Lcom/v1f/app/MainActivity;")
    text = path.read_text(encoding="utf-8")
    marker = f"{ENTRY}->attach(Landroid/app/Activity;)V"
    if marker in text:
        return

    labels_hook = (
        "    invoke-static {p0}, "
        "Lcom/v1f/app/patch/DynamicLabelsEntry;->attach(Landroid/app/Activity;)V"
    )
    if labels_hook not in text:
        raise RuntimeError("DynamicLabelsEntry.attach hook not found in MainActivity")

    interval_hook = f"    invoke-static {{p0}}, {ENTRY}->attach(Landroid/app/Activity;)V"
    text = text.replace(labels_hook, labels_hook + "\n\n" + interval_hook, 1)
    path.write_text(text, encoding="utf-8")


def main() -> int:
    if len(sys.argv) != 2:
        print("Usage: patch_break_interval_dynamic.py <apktool-output-dir>", file=sys.stderr)
        return 2
    root = Path(sys.argv[1])
    patch_break_rules(root)
    patch_application(root)
    patch_main_activity(root)
    print("Configurable 1-24 hour automatic break interval installed; default remains 12h")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
