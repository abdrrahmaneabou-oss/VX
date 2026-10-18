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
    for title, callback in [
        ("◷ مدة الحظر", "QuickDurationAction"),
        ("▣ اختيار التطبيقات", "QuickAppsAction"),
        ("◎ اختيار المواقع", "QuickSitesAction"),
    ]:
        label = ('invoke-static {}, Lcom/v1f/app/patch/HomeCardState;->durationLabel()Ljava/lang/String;\\n\\n    move-result-object v7' if callback == "QuickDurationAction" else f'const-string v7, "{title}"')
        insert += f'''    {label}

    sget-object v8, Lcom/v1f/app/patch/{callback};->INSTANCE:Lcom/v1f/app/patch/{callback};

    sget-object v9, Landroidx/compose/ui/Modifier;->Companion:Landroidx/compose/ui/Modifier$Companion;
    check-cast v9, Landroidx/compose/ui/Modifier;

    const/4 v10, 0x0
    const/4 v11, 0x0
    const/4 v12, 0x0
    move-object v13, v0
    const/4 v14, 0x0
    const/16 v15, 0x3c

    invoke-static/range {{v7 .. v15}}, Lcom/v1f/app/ui/components/CelestialButtonKt;->CelestialButton(Ljava/lang/String;Lkotlin/jvm/functions/Function0;Landroidx/compose/ui/Modifier;ZZZLandroidx/compose/runtime/Composer;II)V

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
    for name, method in [("QuickDurationAction", "openDuration"), ("QuickAppsAction", "openApplications"), ("QuickSitesAction", "openWebsites")]:
        (p.parent / (name + ".smali")).write_text(f'''.class public final Lcom/v1f/app/patch/{name};
.super Lkotlin/jvm/internal/Lambda;
.implements Lkotlin/jvm/functions/Function0;

.field public static final INSTANCE:Lcom/v1f/app/patch/{name};

.method static constructor <clinit>()V
    .locals 1
    new-instance v0, Lcom/v1f/app/patch/{name};
    invoke-direct {{v0}}, Lcom/v1f/app/patch/{name};-><init>()V
    sput-object v0, Lcom/v1f/app/patch/{name};->INSTANCE:Lcom/v1f/app/patch/{name};
    return-void
.end method

.method public constructor <init>()V
    .locals 1
    const/4 v0, 0x0
    invoke-direct {{p0, v0}}, Lkotlin/jvm/internal/Lambda;-><init>(I)V
    return-void
.end method

.method public invoke()Ljava/lang/Object;
    .locals 1
    invoke-static {{}}, Lcom/v1f/app/patch/HomeQuickNav;->{method}()V
    sget-object v0, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;
    return-object v0
.end method
''')
    nav = p.parent / 'HomeQuickNav.smali'
    nav.write_text('''.class public final Lcom/v1f/app/patch/HomeQuickNav;
.super Ljava/lang/Object;

.field private static screen:Landroidx/compose/runtime/MutableState;
.field private static selected:Landroidx/compose/runtime/MutableState;

.method public static attach(Landroidx/compose/runtime/MutableState;Landroidx/compose/runtime/MutableState;)V
    .locals 0
    sput-object p0, Lcom/v1f/app/patch/HomeQuickNav;->screen:Landroidx/compose/runtime/MutableState;
    sput-object p1, Lcom/v1f/app/patch/HomeQuickNav;->selected:Landroidx/compose/runtime/MutableState;
    return-void
.end method

.method public static openApplications()V
    .locals 2
    sget-object v0, Lcom/v1f/app/patch/HomeQuickNav;->screen:Landroidx/compose/runtime/MutableState;
    if-eqz v0, :done
    sget-object v1, Lcom/v1f/app/ui/V1FScreen;->Applications:Lcom/v1f/app/ui/V1FScreen;
    invoke-interface {v0, v1}, Landroidx/compose/runtime/MutableState;->setValue(Ljava/lang/Object;)V
    :done
    return-void
.end method

.method public static openWebsites()V
    .locals 2
    sget-object v0, Lcom/v1f/app/patch/HomeQuickNav;->screen:Landroidx/compose/runtime/MutableState;
    if-eqz v0, :done
    sget-object v1, Lcom/v1f/app/ui/V1FScreen;->Websites:Lcom/v1f/app/ui/V1FScreen;
    invoke-interface {v0, v1}, Landroidx/compose/runtime/MutableState;->setValue(Ljava/lang/Object;)V
    :done
    return-void
.end method

.method public static openDuration()V
    .locals 2
    sget-object v0, Lcom/v1f/app/patch/HomeQuickNav;->selected:Landroidx/compose/runtime/MutableState;
    if-eqz v0, :done
    invoke-interface {v0}, Landroidx/compose/runtime/MutableState;->getValue()Ljava/lang/Object;
    move-result-object v0
    check-cast v0, Ljava/util/Set;
    if-eqz v0, :done
    invoke-static {v0}, Lcom/v1f/app/patch/DurationActivity;->open(Ljava/util/Set;)V
    :done
    return-void
.end method
''')
    host = list(root.rglob('V1FAppKt$V1FApp$2$1.smali'))
    if len(host) != 1:
        raise RuntimeError(f'Expected one navigation host, got {len(host)}')
    hp = host[0]
    hs = hp.read_text()
    constructor_anchor = '    iput-object p4, p0, Lcom/v1f/app/ui/V1FAppKt$V1FApp$2$1;->$selectedPackages$delegate:Landroidx/compose/runtime/MutableState;'
    if hs.count(constructor_anchor) != 1:
        raise RuntimeError('Navigation state constructor anchor not found')
    hs = hs.replace(constructor_anchor, constructor_anchor + '''

    invoke-static {p1, p4}, Lcom/v1f/app/patch/HomeQuickNav;->attach(Landroidx/compose/runtime/MutableState;Landroidx/compose/runtime/MutableState;)V''', 1)
    hp.write_text(hs)
    print('Protection list + three direct native home cards wired to original navigation')

if __name__ == '__main__':
    main(Path(sys.argv[1]))
