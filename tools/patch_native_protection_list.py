#!/usr/bin/env python3
"""Insert a native CelestialButton into the original HomeScreen Compose Column."""
from pathlib import Path
import sys

def main(root):
    matches = list(root.rglob('HomeScreenKt$HomeScreen$3$1$8.smali'))
    if len(matches) != 1:
        raise RuntimeError(f'Expected original home action Column once; got {len(matches)}')
    p = matches[0]
    s = p.read_text()
    if 'V2F_PROTECTION_LIST_COMPOSE' in s:
        return
    anchor = '''    const v7, 0x67fe897f

    invoke-interface {v0, v7}, Landroidx/compose/runtime/Composer;->startReplaceGroup(I)V'''
    if s.count(anchor) != 1:
        raise RuntimeError('Original home actions boundary not found uniquely')
    insert = '''    # V2F_PROTECTION_LIST_COMPOSE: directly after the main setup button, before conditional actions
    const-string v7, "🛡 قائمة الحماية"

    sget-object v8, Lcom/v1f/app/patch/ProtectionListAction;->INSTANCE:Lcom/v1f/app/patch/ProtectionListAction;

    sget-object v9, Landroidx/compose/ui/Modifier;->Companion:Landroidx/compose/ui/Modifier$Companion;
    check-cast v9, Landroidx/compose/ui/Modifier;

    const/4 v10, 0x0
    const/4 v11, 0x0
    const/4 v12, 0x0
    move-object v13, v0
    const/4 v14, 0x0
    const/16 v15, 0x3c

    invoke-static/range {v7 .. v15}, Lcom/v1f/app/ui/components/CelestialButtonKt;->CelestialButton(Ljava/lang/String;Lkotlin/jvm/functions/Function0;Landroidx/compose/ui/Modifier;ZZZLandroidx/compose/runtime/Composer;II)V

'''
    s = s.replace(anchor, insert + anchor, 1)
    p.write_text(s)
    callback = p.parent / 'ProtectionListAction.smali'
    callback.write_text('''.class public final Lcom/v1f/app/patch/ProtectionListAction;
.super Lkotlin/jvm/internal/Lambda;
.implements Lkotlin/jvm/functions/Function0;

.field public static final INSTANCE:Lcom/v1f/app/patch/ProtectionListAction;

.method static constructor <clinit>()V
    .locals 1
    new-instance v0, Lcom/v1f/app/patch/ProtectionListAction;
    invoke-direct {v0}, Lcom/v1f/app/patch/ProtectionListAction;-><init>()V
    sput-object v0, Lcom/v1f/app/patch/ProtectionListAction;->INSTANCE:Lcom/v1f/app/patch/ProtectionListAction;
    return-void
.end method

.method public constructor <init>()V
    .locals 1
    const/4 v0, 0x0
    invoke-direct {p0, v0}, Lkotlin/jvm/internal/Lambda;-><init>(I)V
    return-void
.end method

.method public invoke()Ljava/lang/Object;
    .locals 1
    invoke-static {}, Lcom/v1f/app/patch/DynamicLabelsEntry;->openFromCompose()V
    sget-object v0, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;
    return-object v0
.end method
''')
    print('Native CelestialButton inserted into original HomeScreen Column')

if __name__ == '__main__':
    main(Path(sys.argv[1]))
