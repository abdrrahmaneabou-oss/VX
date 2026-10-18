package com.v1f.app.patch;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * One-time defaults for a truly fresh install only.
 * Existing installs, and all user selections, are left untouched.
 */
public final class DefaultSelections {
    private static final String[] APPS = {
        "com.twitter.android",
        "com.reddit.frontpage",
        "moe.shizuku.privileged.api",
        "com.aether.shizukubridgeX",
        "com.aether.shizukubridge",
        "com.anas.guardbridge",
        "com.blockerhero",
        "com.arlosoft.macrodroid",
        "com.vk.vkvideo",
        "com.virekainteractive.adbshell"
    };
    private static final String[] SITES = {"x.com", "reddit.com"};
    private static final String FLAG = "initialized_v1";

    private DefaultSelections() {}

    public static void apply() {
        try {
            Class<?> graphClass = Class.forName("com.v1f.app.AppGraph");
            Object graph = graphClass.getField("INSTANCE").get(null);
            Application app = (Application) graphClass.getMethod("getApplication").invoke(graph);
            SharedPreferences flags = app.getSharedPreferences("vx_default_selections", Context.MODE_PRIVATE);
            if (flags.getBoolean(FLAG, false)) return;
            // The same package has already been updated: do not add new defaults
            // to an existing user's selection, even if it happens to be empty.
            android.content.pm.PackageInfo own = app.getPackageManager().getPackageInfo(app.getPackageName(), 0);
            boolean fresh = own.lastUpdateTime <= own.firstInstallTime + 2000L;
            if (fresh) {
                SharedPreferences remembered = app.getSharedPreferences("v2f_remembered_apps", Context.MODE_PRIVATE);
                if (!remembered.contains("selected_packages")) {
                    Set<String> selected = new HashSet<>();
                    PackageManager pm = app.getPackageManager();
                    for (String pkg : APPS) {
                        try {
                            pm.getApplicationInfo(pkg, 0);
                            selected.add(pkg);
                        } catch (PackageManager.NameNotFoundException ignored) {}
                    }
                    remembered.edit().putStringSet("selected_packages", selected).commit();
                }
                SharedPreferences sitePrefs = app.getSharedPreferences("v1f_blocked_sites", Context.MODE_PRIVATE);
                if (!sitePrefs.contains("domains")) {
                    Object repository = graphClass.getMethod("getBlockedSitesRepository").invoke(graph);
                    Method add = repository.getClass().getMethod("add", String.class);
                    for (String domain : SITES) add.invoke(repository, domain);
                }
            }
            flags.edit().putBoolean(FLAG, true).commit();
        } catch (Throwable ignored) {
            // Do not disrupt startup if OEM package manager or repository is unavailable.
        }
    }
}
