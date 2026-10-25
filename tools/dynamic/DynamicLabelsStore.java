package com.v1f.app.patch;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class DynamicLabelsStore {
    private static final String PREFS = "vx_dynamic_guard_labels";
    private static final String KEY = "blocked_labels";

    private static final String[] DEFAULTS = new String[] {
            "System Languages",
            "Developer options",
            "Security & privacy",
            "More security & privacy",
            "Device admin",
            "DEACTIVATE THIS DEVICE ADMIN APP",
            "Uninstall",
            "DEACTIVATE & UNINSTALL",
            "Reset app preferences",
            "Reset app preferences?",
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

    /*
     * Keep our own live set instead of mutating SettingsClickGuardConfig's
     * Kotlin set. Some builds expose that set as immutable; calling clear()/
     * addAll() on it caused the editor to crash and also left protection using
     * stale values. The original getter is patched at build time to read this
     * store instead.
     */
    private static volatile Set<String> current = immutableCopy(defaults());

    private DynamicLabelsStore() {}

    public static LinkedHashSet<String> defaults() {
        return new LinkedHashSet<>(Arrays.asList(DEFAULTS));
    }

    public static LinkedHashSet<String> load(Context context) {
        if (context == null) return defaults();
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!prefs.contains(KEY)) return defaults();
        Set<String> stored = prefs.getStringSet(KEY, null);
        return sanitize(stored == null ? defaults() : stored);
    }

    public static void init(Context context) {
        try {
            current = immutableCopy(load(context));
        } catch (Throwable ignored) {
            current = immutableCopy(defaults());
        }
    }

    public static void save(Context context, Set<String> labels) {
        if (!StrictBlockPolicy.allowsAll()) throw new IllegalStateException("Settings are locked during strict blocking");
        LinkedHashSet<String> copy = sanitize(labels);
        if (context != null) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit().putStringSet(KEY, new LinkedHashSet<>(copy)).apply();
        }
        current = immutableCopy(copy);
    }

    public static void reset(Context context) {
        save(context, defaults());
    }

    /** Returns a stable snapshot used by both the original and robust guards. */
    public static Set<String> snapshot() {
        return current;
    }

    private static LinkedHashSet<String> sanitize(Iterable<String> labels) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (labels == null) return out;
        for (String label : labels) {
            if (label == null) continue;
            String value = label.trim();
            if (!value.isEmpty()) out.add(value);
        }
        return out;
    }

    private static Set<String> immutableCopy(Set<String> labels) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(labels));
    }
}
