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
        label = ('invoke-static {}, Lcom/v1f/app/patch/HomeCardState;->durationLabel()Ljava/lang/String;\n\n    move-result-object v7' if callback == "QuickDurationAction" else f'const-string v7, "{title}"')
        action_load = (
            'move-object/from16 v8, p0\n    iget-object v8, v8, Lcom/v1f/app/ui/screens/HomeScreenKt$HomeScreen$3$1$8;->$onPrimaryAction:Lkotlin/jvm/functions/Function0;\n    new-instance v6, Lcom/v1f/app/patch/AppsCardAction;\n    invoke-direct {v6, v8}, Lcom/v1f/app/patch/AppsCardAction;-><init>(Lkotlin/jvm/functions/Function0;)V\n    move-object v8, v6'
            if callback == "QuickAppsAction" else
            'move-object/from16 v8, p0\n    iget-object v8, v8, Lcom/v1f/app/ui/screens/HomeScreenKt$HomeScreen$3$1$8;->$onOpenWebsites:Lkotlin/jvm/functions/Function0;'
            if callback == "QuickSitesAction" else
            f'sget-object v8, Lcom/v1f/app/patch/{callback};->INSTANCE:Lcom/v1f/app/patch/{callback};'
        )
        insert += f'''    {label}

    {action_load}

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
    insert += """    # Separate activation from configuration cards.
    sget-object v7, Landroidx/compose/ui/Modifier;->Companion:Landroidx/compose/ui/Modifier$Companion;
    check-cast v7, Landroidx/compose/ui/Modifier;
    const/16 v8, 0x18
    int-to-float v8, v8
    invoke-static {v8}, Landroidx/compose/ui/unit/Dp;->constructor-impl(F)F
    move-result v8
    invoke-static {v7, v8}, Landroidx/compose/foundation/layout/SizeKt;->height-3ABfNKs(Landroidx/compose/ui/Modifier;F)Landroidx/compose/ui/Modifier;
    move-result-object v7
    const/4 v9, 0x6
    invoke-static {v7, v0, v9}, Landroidx/compose/foundation/layout/SpacerKt;->Spacer(Landroidx/compose/ui/Modifier;Landroidx/compose/runtime/Composer;I)V

    const-string v7, "بدء الحماية"

    sget-object v8, Lcom/v1f/app/patch/QuickStartAction;->INSTANCE:Lcom/v1f/app/patch/QuickStartAction;
    sget-object v9, Landroidx/compose/ui/Modifier;->Companion:Landroidx/compose/ui/Modifier$Companion;
    check-cast v9, Landroidx/compose/ui/Modifier;
    const/16 v10, 0xdc
    int-to-float v10, v10
    invoke-static {v10}, Landroidx/compose/ui/unit/Dp;->constructor-impl(F)F
    move-result v10
    invoke-static {v9, v10}, Landroidx/compose/foundation/layout/SizeKt;->width-3ABfNKs(Landroidx/compose/ui/Modifier;F)Landroidx/compose/ui/Modifier;
    move-result-object v9
    const/4 v10, 0x0
    const/4 v11, 0x0
    const/4 v12, 0x0
    move-object v13, v0
    const/4 v14, 0x0
    const/16 v15, 0x38
    invoke-static/range {v7 .. v15}, Lcom/v1f/app/ui/components/CelestialButtonKt;->CelestialButton(Ljava/lang/String;Lkotlin/jvm/functions/Function0;Landroidx/compose/ui/Modifier;ZZZLandroidx/compose/runtime/Composer;II)V

"""
    s = s.replace(anchor, insert + anchor, 1)
    # The original website shortcut was conditional on full permissions and
    # appears after our independent cards. Suppress only that legacy shortcut;
    # retain its Compose group start/end for a balanced composition.
    legacy = '''    invoke-virtual {v4}, Lcom/v1f/app/model/PermissionSnapshot;->getAllReady()Z

    move-result v4

    if-eqz v4, :cond_6'''
    if s.count(legacy) != 1:
        raise RuntimeError("Legacy conditional website shortcut anchor changed")
    s = s.replace(legacy, '    goto :cond_6', 1)
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
    for name, method in [("QuickDurationAction", "openDuration")]:
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
    for name, method in [("QuickStartAction", "startProtection")]:
        (p.parent / (name + ".smali")).write_text(f""".class public final Lcom/v1f/app/patch/{name};
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
""")
    # Delegate to the device-proven Compose callback, but mark the Apps card
    # origin so the primary callback can distinguish it from Permissions.
    (p.parent / 'AppsCardAction.smali').write_text('''.class public final Lcom/v1f/app/patch/AppsCardAction;
.super Lkotlin/jvm/internal/Lambda;
.implements Lkotlin/jvm/functions/Function0;
.field private final delegate:Lkotlin/jvm/functions/Function0;
.method public constructor <init>(Lkotlin/jvm/functions/Function0;)V
    .locals 1
    const/4 v0, 0x0
    invoke-direct {p0, v0}, Lkotlin/jvm/internal/Lambda;-><init>(I)V
    iput-object p1, p0, Lcom/v1f/app/patch/AppsCardAction;->delegate:Lkotlin/jvm/functions/Function0;
    return-void
.end method
.method public invoke()Ljava/lang/Object;
    .locals 1
    invoke-static {}, Lcom/v1f/app/patch/HomeQuickNav;->markAppsCard()V
    :try_start_delegate
    iget-object v0, p0, Lcom/v1f/app/patch/AppsCardAction;->delegate:Lkotlin/jvm/functions/Function0;
    invoke-interface {v0}, Lkotlin/jvm/functions/Function0;->invoke()Ljava/lang/Object;
    move-result-object v0
    :try_end_delegate
    .catchall {:try_start_delegate .. :try_end_delegate} :clear_and_rethrow
    invoke-static {}, Lcom/v1f/app/patch/HomeQuickNav;->clearAppsCard()V
    return-object v0
    :clear_and_rethrow
    move-exception v0
    invoke-static {}, Lcom/v1f/app/patch/HomeQuickNav;->clearAppsCard()V
    throw v0
.end method
''')
    nav = p.parent / 'HomeQuickNav.smali'
    nav.write_text('''.class public final Lcom/v1f/app/patch/HomeQuickNav;
.super Ljava/lang/Object;

.field private static screen:Landroidx/compose/runtime/MutableState;
.field private static appsCardPending:Z
.field private static selected:Landroidx/compose/runtime/MutableState;\n.field private static pendingStart:Z

.method public static attach(Landroidx/compose/runtime/MutableState;Landroidx/compose/runtime/MutableState;)V
    .locals 0
    sput-object p0, Lcom/v1f/app/patch/HomeQuickNav;->screen:Landroidx/compose/runtime/MutableState;
    sput-object p1, Lcom/v1f/app/patch/HomeQuickNav;->selected:Landroidx/compose/runtime/MutableState;
    return-void
.end method

.method public static markAppsCard()V
    .locals 1
    const/4 v0, 0x1
    sput-boolean v0, Lcom/v1f/app/patch/HomeQuickNav;->appsCardPending:Z
    return-void
.end method

.method public static clearAppsCard()V
    .locals 1
    const/4 v0, 0x0
    sput-boolean v0, Lcom/v1f/app/patch/HomeQuickNav;->appsCardPending:Z
    return-void
.end method

.method public static consumeAppsCard()Z
    .locals 2
    sget-boolean v0, Lcom/v1f/app/patch/HomeQuickNav;->appsCardPending:Z
    const/4 v1, 0x0
    sput-boolean v1, Lcom/v1f/app/patch/HomeQuickNav;->appsCardPending:Z
    return v0
.end method

.method public static startProtection()V
    .locals 2
    # Do not arm a delayed session start if the Compose host is unavailable.
    # Otherwise a later visit to DurationActivity could unexpectedly start it.
    sget-object v0, Lcom/v1f/app/patch/HomeQuickNav;->selected:Landroidx/compose/runtime/MutableState;
    if-eqz v0, :unavailable
    invoke-interface {v0}, Landroidx/compose/runtime/MutableState;->getValue()Ljava/lang/Object;
    move-result-object v0
    if-eqz v0, :unavailable
    const/4 v1, 0x1
    sput-boolean v1, Lcom/v1f/app/patch/HomeQuickNav;->pendingStart:Z
    check-cast v0, Ljava/util/Set;
    invoke-static {v0}, Lcom/v1f/app/patch/DurationActivity;->open(Ljava/util/Set;)V
    return-void
    :unavailable
    const/4 v1, 0x0
    sput-boolean v1, Lcom/v1f/app/patch/HomeQuickNav;->pendingStart:Z
    return-void
.end method

.method public static consumeStart()Z
    .locals 2
    sget-boolean v0, Lcom/v1f/app/patch/HomeQuickNav;->pendingStart:Z
    const/4 v1, 0x0
    sput-boolean v1, Lcom/v1f/app/patch/HomeQuickNav;->pendingStart:Z
    return v0
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
