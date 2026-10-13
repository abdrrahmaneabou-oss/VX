package com.v1f.app.patch;

import android.content.Context;
import android.content.SharedPreferences;

import com.v1f.app.protection.modules.settings.SettingsClickGuardConfig;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public final class DynamicLabelsStore {
    private static final String PREFS = "vx_dynamic_guard_labels";
    private static final String KEY = "blocked_labels";

    private static final String[] DEFAULTS = new String[] {
            "System Languages",
            "Developer options",
            "Device admin",
            "DEACTIVATE THIS DEVICE ADMIN APP",
            "Uninstall",
            "DEACTIVATE & UNINSTALL",
            "Reset app preferences",
            "Background running",
            "V1F App Guard",
            "V1F Device Protection",
            "SAFEX Accessibility",
            "SAFEX Device Admin",
            "About phone",
            "Build number",
            "Wireless debugging",
            "BlockerHero",
            "Reset options",
            "Add a language",
            "Restore Applications"
    };

    private DynamicLabelsStore() {}

    public static LinkedHashSet<String> defaults() {
        return new LinkedHashSet<>(Arrays.asList(DEFAULTS));
    }

    public static LinkedHashSet<String> load(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!prefs.contains(KEY)) return defaults();
        Set<String> stored = prefs.getStringSet(KEY, null);
        return stored == null ? defaults() : new LinkedHashSet<>(stored);
    }

    public static void init(Context context) {
        apply(load(context));
    }

    public static void save(Context context, Set<String> labels) {
        LinkedHashSet<String> copy = new LinkedHashSet<>(labels);
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putStringSet(KEY, copy).apply();
        apply(copy);
    }

    public static void reset(Context context) {
        save(context, defaults());
    }

    private static void apply(Set<String> labels) {
        Set<String> live = SettingsClickGuardConfig.INSTANCE.getBlockedLabels();
        live.clear();
        live.addAll(labels);
    }
}
