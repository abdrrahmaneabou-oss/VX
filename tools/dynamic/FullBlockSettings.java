package com.v1f.app.patch;

import android.content.Context;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Separate from the normal session's selected applications. */
public final class FullBlockSettings {
    private static final String PREFS = "vx_hourly_exemptions";
    private static final String KEY = "exempt_packages";
    private FullBlockSettings() {}

    public static Set<String> load(Context context) {
        return new HashSet<>(context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getStringSet(KEY, Collections.<String>emptySet()));
    }

    public static boolean save(Context context, Set<String> exemptions) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putStringSet(KEY, new HashSet<>(exemptions)).commit();
    }

    public static boolean isExempt(Context context, String pkg) {
        return !FullBlockRules.included(load(context), pkg);
    }
}
