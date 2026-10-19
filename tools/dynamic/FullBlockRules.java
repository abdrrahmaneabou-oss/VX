package com.v1f.app.patch;

import java.util.Calendar;
import java.util.Set;
import java.util.TimeZone;

/** Pure rules shared by the Android runtime and boundary tests. */
public final class FullBlockRules {
    private FullBlockRules() {}

    public static boolean windowActive(long now, TimeZone zone, int minutes) {
        Calendar clock = Calendar.getInstance(zone);
        clock.setTimeInMillis(now);
        int duration = Math.max(1, Math.min(45, minutes));
        return clock.get(Calendar.MINUTE) >= 60 - duration;
    }

    public static boolean included(Set<String> exemptions, String pkg) {
        return pkg != null && !exemptions.contains(pkg);
    }
}
