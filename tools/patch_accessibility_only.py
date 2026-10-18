#!/usr/bin/env python3
"""Require Accessibility only for V1F's permission readiness gate.

Preserve Device Admin and background permission snapshots, permission setup tiles,
and their actual status counts. Do not spoof Android system permissions.
"""
from pathlib import Path
import sys

root = Path(sys.argv[1])
matches = list(root.rglob('com/v1f/app/model/PermissionSnapshot.smali'))
if len(matches) != 1:
    raise RuntimeError(f'Expected exactly one PermissionSnapshot.smali: {matches}')
path = matches[0]
text = path.read_text()
start = text.find('.method public final getAllReady()Z')
end = text.find('.end method', start)
if start < 0 or end < 0:
    raise RuntimeError('Cannot locate getAllReady permission gate')
end += len('.end method')
original = text[start:end]
if '->getAccessibilityEnabled()Z' in original and '->getCompletedCount()I' not in original:
    print('Accessibility-only readiness already applied')
    sys.exit(0)
if '->getCompletedCount()I' not in original or 'const/4 v1, 0x3' not in original:
    raise RuntimeError('Unexpected readiness implementation; refusing unsafe patch')
replacement = """.method public final getAllReady()Z
    .locals 1

    # Only Accessibility is required to navigate, select apps and start blocking.
    # Device Admin and background optimizations remain separately optional.
    invoke-virtual {p0}, Lcom/v1f/app/model/PermissionSnapshot;->getAccessibilityEnabled()Z

    move-result v0

    return v0
.end method"""
path.write_text(text[:start] + replacement + text[end:])
print('PermissionSnapshot.getAllReady now requires only Accessibility')
