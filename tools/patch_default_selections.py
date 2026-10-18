#!/usr/bin/env python3
"""Initialize user-editable app/site defaults once after AppGraph is ready."""
from pathlib import Path
import sys

root = Path(sys.argv[1])
paths = list(root.rglob("com/v1f/app/AppGraph.smali"))
if len(paths) != 1:
    raise RuntimeError("Expected one AppGraph.smali")
path = paths[0]
data = path.read_text()
signature = ".method public final initialize(Landroid/app/Application;)V"
start = data.find(signature)
end = data.find(".end method", start)
if start < 0 or end < 0:
    raise RuntimeError("Cannot find AppGraph.initialize")
section = data[start:end]
needle = "    return-void"
if section.count(needle) != 1:
    raise RuntimeError("Unexpected AppGraph.initialize return paths: " + str(section.count(needle)))
section = section.replace(needle,
    "    invoke-static {}, Lcom/v1f/app/patch/DefaultSelections;->apply()V\n\n" + needle)
path.write_text(data[:start] + section + data[end:])
print("One-time optional defaults hooked after AppGraph.initialize")
