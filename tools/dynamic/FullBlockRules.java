package com.v1f.app.patch;

import java.util.Calendar;
import java.util.Set;
import java.util.TimeZone;

/** Pure rules shared by the Android runtime and boundary tests. */
public final class FullBlockRules {
    private FullBlockRules() {}

    public static boolean windowActive(long now, TimeZone zone, int minutes) {
        return remainingMillis(now, zone, minutes) > 0;
    }

    public static long remainingMillis(long now, TimeZone zone, int minutes) {
        Calendar clock = Calendar.getInstance(zone);
        clock.setTimeInMillis(now);
        int duration = Math.max(1, Math.min(45, minutes));
        if (clock.get(Calendar.MINUTE) < 60 - duration) return 0;
        return (60 - clock.get(Calendar.MINUTE)) * 60000L
                - clock.get(Calendar.SECOND) * 1000L - clock.get(Calendar.MILLISECOND);
    }

    public static String countdownText(long remaining) {
        if (remaining <= 0) return "";
        long minutes = (remaining + 59999L) / 60000L;
        if (minutes == 1) return "الحظر الشامل: دقيقة واحدة متبقية";
        if (minutes == 2) return "الحظر الشامل: دقيقتان متبقيتان";
        return "الحظر الشامل: " + minutes + (minutes <= 10 ? " دقائق متبقية" : " دقيقة متبقية");
    }

    public static boolean included(Set<String> exemptions, String pkg) {
        return pkg != null && !exemptions.contains(pkg);
    }
}
