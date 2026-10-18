#!/usr/bin/env python3
"""Fail the build on known home navigation regressions; not a device test."""
from pathlib import Path
import re
import sys


def verify(root):
    def read(name):
        paths = list(root.rglob(name))
        assert len(paths) == 1, (name, paths)
        return paths[0].read_text()

    def method(text, name):
        found = re.search(r'(?ms)^\.method [^\n]* ' + re.escape(name)
                          + r'\([^\n]*\n.*?^\.end method', text)
        assert found, name
        return found.group()

    permissions = read('PermissionsScreenKt.smali')
    for p in root.rglob('*PermissionsScreen*.smali'):
        text = p.read_text()
        assert 'التطبيقات' not in text
        assert 'التطبيقات'.encode('unicode_escape').decode() not in text, p
    assert permissions.count('PermissionTileKt;->PermissionTile-') == 3
    assert 'CelestialButtonKt;->CelestialButton(' not in permissions

    host = read('V1FAppKt$V1FApp$2$1$2.smali')
    primary = method(host, 'invoke$lambda$3$lambda$2')
    assert 'getAllReady' not in primary
    assert 'HomeQuickNav;->consumeAppsCard()Z' in primary
    assert primary.count('V1FScreen;->Applications:') == 1
    assert primary.count('V1FScreen;->Permissions:') == 1
    assert 'invoke-static {p1, v0}' in primary
    assert re.search(r'move-object v4, v3\s+invoke-static/range \{v1 \.\. v8\}.*PermissionsScreen\(', host)
    save = method(host, 'invoke$lambda$13$lambda$12')
    assert 'access$V1FApp$lambda$7(' in save and 'V1FScreen;->Home:' in save
    assert 'DurationActivity;->open(' not in save
    assert 'startSession' not in save

    home = read('HomeScreenKt$HomeScreen$3$1$8.smali')
    first = home.index('CelestialButtonKt;->CelestialButton(')
    assert 'QuickPermissionsAction' not in home[:first]
    assert home.count('AppsCardAction;-><init>(Lkotlin/jvm/functions/Function0;)V') == 1
    assert home.count('->$onPrimaryAction:Lkotlin/jvm/functions/Function0;') >= 2
    nav = read('HomeQuickNav.smali')
    # V1FScreen is package-private: patch-package helpers cannot reference it.
    assert 'Lcom/v1f/app/ui/V1FScreen;' not in nav
    consume = method(nav, 'consumeAppsCard')
    assert 'const/4 v1, 0x0' in consume and 'sput-boolean v1' in consume
    clear = method(nav, 'clearAppsCard')
    assert 'const/4 v0, 0x0' in clear and 'sput-boolean v0' in clear
    action = read('AppsCardAction.smali')
    assert '.implements Lkotlin/jvm/functions/Function0;' in action
    invoke = method(action, 'invoke')
    assert 'Function0;->invoke()Ljava/lang/Object;' in invoke
    assert '.catchall' in invoke and 'move-exception v0' in invoke
    assert invoke.count('HomeQuickNav;->clearAppsCard()V') == 2
    assert 'throw v0' in invoke  # Clean up, but never hide a real callback failure.
    print('PASS: permission-only UI, 3 tiles, callback routing, marker cleanup, apps save, enum access')


if __name__ == '__main__':
    verify(Path(sys.argv[1]))
