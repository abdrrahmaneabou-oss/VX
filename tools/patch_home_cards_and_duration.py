#!/usr/bin/env python3
"""Separate configuration saving from session activation on native V1F screens."""
from pathlib import Path
import re, sys

def unique(root, name):
    files = list(root.rglob(name))
    if len(files) != 1: raise RuntimeError(f"Expected one {name}, got {len(files)}")
    return files[0]

def main(root):
    home = unique(root, 'HomeScreenKt$HomeScreen$3$1$8.smali')
    s = home.read_text()
    if s.count('Lcom/v1f/app/ui/components/CelestialButtonKt;->CelestialButton(') < 2:
        raise RuntimeError("Unexpected home button structure")
    s = s.replace('const-string v0, "\\u0627\\u062e\\u062a\\u064a\\u0627\\u0631 \\u0627\\u0644\\u062a\\u0637\\u0628\\u064a\\u0642\\u0627\\u062a"', 'const-string v0, "الصلاحيات"')
    s = s.replace('const-string v0, "\\u0627\\u0628\\u062f\\u0623 \\u0625\\u0639\\u062f\\u0627\\u062f \\u0627\\u0644\\u062d\\u0645\\u0627\\u064a\\u0629"', 'const-string v0, "الصلاحيات"')
    first = '    invoke-static/range {v7 .. v15}, Lcom/v1f/app/ui/components/CelestialButtonKt;->CelestialButton('
    if first not in s: raise RuntimeError("Primary button anchor missing")
    s = s.replace(first, '    sget-object v8, Lcom/v1f/app/patch/QuickPermissionsAction;->INSTANCE:Lcom/v1f/app/patch/QuickPermissionsAction;\n\n' + first, 1)
    home.write_text(s)

    duration = unique(root, 'DurationActivity.smali')
    s = duration.read_text()
    callback = re.search(r'(?ms)^\.method synthetic lambda\$5\$com-v1f-app-patch-DurationActivity\(Landroid/view/View;\)V.*?^\.end method', s)
    if not callback: raise RuntimeError("Duration save click callback not found")
    save = """.method synthetic lambda$5$com-v1f-app-patch-DurationActivity(Landroid/view/View;)V
    .locals 4
    :try_start_save
    invoke-direct {p0}, Lcom/v1f/app/patch/DurationActivity;->readFields()[I
    move-result-object v0
    iget-object v1, p0, Lcom/v1f/app/patch/DurationActivity;->values:[I
    const/4 v2, 0x0
    const/4 v3, 0x3
    invoke-static {v0, v2, v1, v2, v3}, Ljava/lang/System;->arraycopy(Ljava/lang/Object;ILjava/lang/Object;II)V
    invoke-direct {p0}, Lcom/v1f/app/patch/DurationActivity;->savePreferences()V
    const-string v0, "تم حفظ المدة"
    const/4 v1, 0x0
    invoke-static {p0, v0, v1}, Landroid/widget/Toast;->makeText(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;
    move-result-object v0
    invoke-virtual {v0}, Landroid/widget/Toast;->show()V
    invoke-virtual {p0}, Lcom/v1f/app/patch/DurationActivity;->finish()V
    :try_end_save
    .catch Ljava/lang/IllegalArgumentException; {:try_start_save .. :try_end_save} :invalid
    return-void
    :invalid
    move-exception v0
    const-string v0, "المدة غير صالحة"
    const/4 v1, 0x0
    invoke-static {p0, v0, v1}, Landroid/widget/Toast;->makeText(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;
    move-result-object v0
    invoke-virtual {v0}, Landroid/widget/Toast;->show()V
    return-void
.end method"""
    s = s[:callback.start()] + save + s[callback.end():]
    # Only the duration editor button is renamed; activation has its own home card.
    old_button = 'const-string v1, "\\u0628\\u062f\\u0621 \\u0627\\u0644\\u062d\\u0645\\u0627\\u064a\\u0629"'
    if s.count(old_button) != 1: raise RuntimeError("Duration action label missing")
    s = s.replace(old_button, 'const-string v1, "حفظ"', 1)
    # The dedicated start action sets a flag before opening this Activity.
    if '.method protected onResume()V' in s: raise RuntimeError("Unexpected pre-existing onResume")
    s += """
.method protected onResume()V
    .locals 1
    invoke-super {p0}, Landroid/app/Activity;->onResume()V
    invoke-static {}, Lcom/v1f/app/patch/HomeQuickNav;->consumeStart()Z
    move-result v0
    if-eqz v0, :done
    invoke-direct {p0}, Lcom/v1f/app/patch/DurationActivity;->startSession()V
    :done
    return-void
.end method
"""
    # Device Admin is optional for starting a session.
    s = s.replace('invoke-virtual {v6}, Lcom/v1f/app/model/PermissionSnapshot;->getAllReady()Z', 'invoke-static {}, Lcom/v1f/app/patch/HomeCardState;->accessibilityReady()Z')
    duration.write_text(s)
    print("Home permissions, duration save and independent start hooks applied")

if __name__ == '__main__':
    main(Path(sys.argv[1]))
