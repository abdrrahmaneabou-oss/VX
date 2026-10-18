#!/usr/bin/env python3
"""Separate configuration saving from session activation on native V1F screens."""
from pathlib import Path
import re, sys

def unique(root, name):
    files = list(root.rglob(name))
    if len(files) != 1: raise RuntimeError(f"Expected one {name}, got {len(files)}")
    return files[0]

def permissions_only(root):
    """Remove the old wizard CTA without disturbing its Compose group pair."""
    p = unique(root, 'PermissionsScreenKt.smali')
    s = p.read_text()
    def literal(text):
        return text.encode('unicode_escape').decode('ascii')
    title = '"' + literal('اختيار التطبيقات') + '"'
    if s.count(title) != 1:
        raise RuntimeError('Expected one legacy apps CTA in Permissions')
    label = s.index(title)
    start = s.rfind('    invoke-virtual/range {p0 .. p0}, Lcom/v1f/app/model/PermissionSnapshot;->getAllReady()Z', 0, label)
    button = '    invoke-static/range {v9 .. v17}, Lcom/v1f/app/ui/components/CelestialButtonKt;->CelestialButton('
    end = s.find('\n', s.index(button, label))
    if start < 0 or end < 0:
        raise RuntimeError('Permissions CTA boundaries changed')
    removed = s[start:end]
    if '->startReplaceGroup(' in removed or '->endReplaceGroup(' in removed:
        raise RuntimeError('Refusing to unbalance Permissions composition')
    outside = s[:start] + s[end:]
    for target in re.findall(r'^\s*(:\w+)\s*$', removed, re.M):
        if re.search(re.escape(target) + r'\b', outside):
            raise RuntimeError('Permissions CTA has an external branch target')
    s = s[:start] + '    # V2F_PERMISSIONS_ONLY: no application-selection CTA' + s[end:]
    for before, after in [
        ('فعّل الطبقات الثلاث لتشغيل اختيار التطبيقات والحظر الصارم.', 'اضبط الصلاحيات الثلاث من البطاقات أدناه.'),
        ('مراقبة التطبيقات وتنفيذ شاشة الحظر جاهزة.', 'إمكانية الوصول وتنفيذ شاشة الحظر جاهزان.'),
    ]:
        if s.count(literal(before)) != 1:
            raise RuntimeError('Permissions explanation changed')
        s = s.replace(literal(before), literal(after), 1)
    if literal('التطبيقات') in s or 'التطبيقات' in s:
        raise RuntimeError('Applications text remains in Permissions')
    p.write_text(s)

def main(root):
    permissions_only(root)
    home = unique(root, 'HomeScreenKt$HomeScreen$3$1$8.smali')
    s = home.read_text()
    if s.count('Lcom/v1f/app/ui/components/CelestialButtonKt;->CelestialButton(') < 2:
        raise RuntimeError("Unexpected home button structure")
    s = s.replace('const-string v0, "\\u0627\\u062e\\u062a\\u064a\\u0627\\u0631 \\u0627\\u0644\\u062a\\u0637\\u0628\\u064a\\u0642\\u0627\\u062a"', 'invoke-static {}, Lcom/v1f/app/patch/HomeCardState;->permissionLabel()Ljava/lang/String;\n\n    move-result-object v0')
    s = s.replace('const-string v0, "\\u0627\\u0628\\u062f\\u0623 \\u0625\\u0639\\u062f\\u0627\\u062f \\u0627\\u0644\\u062d\\u0645\\u0627\\u064a\\u0629"', 'invoke-static {}, Lcom/v1f/app/patch/HomeCardState;->permissionLabel()Ljava/lang/String;\n\n    move-result-object v0')
    first = '    invoke-static/range {v7 .. v15}, Lcom/v1f/app/ui/components/CelestialButtonKt;->CelestialButton('
    if first not in s: raise RuntimeError("Primary button anchor missing")
    # Preserve the original callback and its Compose capture. Route it below
    # within the ui package, where the package-private V1FScreen is accessible.
    # Keep the existing accessibility-dependent button styling intact.
    s = s.replace(first, '''    invoke-static {}, Lcom/v1f/app/patch/HomeCardState;->accessibilityReady()Z
    move-result v11
    xor-int/lit8 v11, v11, 0x1
    const/16 v15, 0x2c

''' + first, 1)
    home.write_text(s)

    # Reuse the original Permissions onBack callback for its completion action.
    # This removes the old Permissions -> Applications forced progression.
    host = unique(root, 'V1FAppKt$V1FApp$2$1$2.smali')
    hs = host.read_text()
    # The first home card and Apps card share the original Compose callback.
    # AppsCardAction marks the source before invoking that proven callback.
    # Consume the marker here: only an explicit Apps-card click opens Apps;
    # every ordinary first-card click opens Permissions, regardless of status.
    primary_start = hs.find('.method private static final invoke$lambda$3$lambda$2(')
    primary_end = hs.find('.end method', primary_start)
    if primary_start < 0 or primary_end < 0:
        raise RuntimeError("Original primary home callback missing")
    primary = hs[primary_start:primary_end]
    call_start = primary.find('    invoke-static {p0}, Lcom/v1f/app/ui/V1FAppKt;->access$V1FApp$lambda$1(')
    call_end = primary.find('    invoke-static {p1, v0}, Lcom/v1f/app/ui/V1FAppKt;->access$V1FApp$lambda$4(', call_start)
    if call_start < 0 or call_end < 0:
        raise RuntimeError("Original primary callback body changed")
    original_branch = primary[call_start:call_end]
    if (original_branch.count('V1FScreen;->Applications:') != 1 or
            original_branch.count('V1FScreen;->Permissions:') != 1):
        raise RuntimeError("Unexpected original primary destinations")
    primary = primary[:call_start] + '''    invoke-static {}, Lcom/v1f/app/patch/HomeQuickNav;->consumeAppsCard()Z
    move-result v0
    if-eqz v0, :permissions_card
    sget-object v0, Lcom/v1f/app/ui/V1FScreen;->Applications:Lcom/v1f/app/ui/V1FScreen;
    goto :card_destination
    :permissions_card
    sget-object v0, Lcom/v1f/app/ui/V1FScreen;->Permissions:Lcom/v1f/app/ui/V1FScreen;
    :card_destination
''' + primary[call_end:]
    hs = hs[:primary_start] + primary + hs[primary_end:]
    invocation = '    invoke-static/range {v1 .. v8}, Lcom/v1f/app/ui/screens/PermissionsScreenKt;->PermissionsScreen('
    if hs.count(invocation) != 1:
        raise RuntimeError("Permissions screen callsite changed")
    hs = hs.replace(invocation, '    move-object v4, v3\n\n' + invocation, 1)
    # The native apps completion callback already persists the selected set
    # and navigates Home. Its extra DurationActivity launch is legacy wizard
    # behavior and must not happen on Save.
    old_launch = '    invoke-static {p2}, Lcom/v1f/app/patch/DurationActivity;->open(Ljava/util/Set;)V'
    if hs.count(old_launch) != 1:
        raise RuntimeError("Apps completion legacy duration launch not found uniquely")
    hs = hs.replace(old_launch, '    # V2F_APPS_SAVE_RETURN_HOME: preserve selection and Home transition', 1)
    # Fail closed if a future upstream APK changes the save callback. We must
    # never silently remove its persistence or return-home operations.
    callback_start = hs.find('.method private static final invoke$lambda$13$lambda$12(')
    callback_end = hs.find('.end method', callback_start)
    if callback_start < 0 or callback_end < 0:
        raise RuntimeError("Apps completion callback not found")
    callback_body = hs[callback_start:callback_end]
    if ('access$V1FApp$lambda$7(' not in callback_body or
            'V1FScreen;->Home:' not in callback_body or
            'access$V1FApp$lambda$4(' not in callback_body or
            'DurationActivity;->open(' in callback_body):
        raise RuntimeError("Apps save/return-home invariants violated")
    if 'move-object v4, v3\n\n' + invocation not in hs:
        raise RuntimeError("Permissions completion must reuse the back callback")
    host.write_text(hs)

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
    invoke-direct {p0}, Lcom/v1f/app/patch/DurationActivity;->savePreferences()V\n    invoke-static {}, Lcom/v1f/app/patch/HomeCardState;->refreshHome()V
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
