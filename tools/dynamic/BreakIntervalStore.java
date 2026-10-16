package com.v1f.app.patch;

import android.content.Context;
import android.content.SharedPreferences;

/** Runtime-configurable break interval, independent from the protected-label list. */
public final class BreakIntervalStore {
    private static final String PREFS = "vx_break_interval";
    private static final String KEY_HOURS = "interval_hours";
    private static final int DEFAULT_HOURS = 24;
    private static final int MIN_HOURS = 1;
    private static final int MAX_HOURS = 24;
    private static volatile Context appContext;

    private BreakIntervalStore() {}

    public static void init(Context context) {
        if (context != null) appContext = context.getApplicationContext();
    }

    public static int getHours() {
        Context context = appContext;
        if (context == null) return DEFAULT_HOURS;
        int hours = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt(KEY_HOURS, DEFAULT_HOURS);
        return clamp(hours);
    }

    /** Called by the original BreakRules bytecode; result is milliseconds. */
    public static long getIntervalMs() {
        return getHours() * 3_600_000L;
    }

    public static int adjustHours(int delta) {
        if (!StrictBlockPolicy.allowsAll()) return getHours();
        int next = clamp(getHours() + delta);
        Context context = appContext;
        if (context != null) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit().putInt(KEY_HOURS, next).apply();
        }
        return next;
    }

    public static int setHours(int hours) {
        if (!StrictBlockPolicy.allowsAll()) return getHours();
        int next = clamp(hours);
        Context context = appContext;
        if (context != null) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit().putInt(KEY_HOURS, next).apply();
        }
        return next;
    }

    private static int clamp(int hours) {
        return Math.max(MIN_HOURS, Math.min(MAX_HOURS, hours));
    }
}
