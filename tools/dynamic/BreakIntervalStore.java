package com.vx.app.patch;

import android.content.Context;
import android.content.SharedPreferences;

public final class BreakIntervalStore {
    private static final String PREFS = "vx_break_interval";
    private static final String KEY_HOURS = "interval_hours";
    private static final int MIN_HOURS = 1;
    private static final int MAX_HOURS = 24;
    private static final int DEFAULT_HOURS = 12;
    private static final long HOUR_MS = 3_600_000L;

    private static volatile int currentHours = DEFAULT_HOURS;

    private BreakIntervalStore() {}

    public static void init(Context context) {
        int value = DEFAULT_HOURS;
        try {
            if (context != null) {
                SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
                value = prefs.getInt(KEY_HOURS, DEFAULT_HOURS);
            }
        } catch (Throwable ignored) {
            value = DEFAULT_HOURS;
        }
        currentHours = clamp(value);
    }

    public static int getHours() {
        return clamp(currentHours);
    }

    public static void setHours(Context context, int hours) {
        int value = clamp(hours);
        currentHours = value;
        if (context != null) {
            try {
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                        .edit()
                        .putInt(KEY_HOURS, value)
                        .apply();
            } catch (Throwable ignored) {
                // Keep the live value even if persistence is temporarily unavailable.
            }
        }
    }

    public static long getIntervalMs() {
        return ((long) getHours()) * HOUR_MS;
    }

    private static int clamp(int value) {
        if (value < MIN_HOURS) return MIN_HOURS;
        if (value > MAX_HOURS) return MAX_HOURS;
        return value;
    }
}
