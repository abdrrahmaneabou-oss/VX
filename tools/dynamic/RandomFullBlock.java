package com.v1f.app.patch;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.accessibility.AccessibilityEvent;
import java.lang.reflect.Method;
import java.security.SecureRandom;

/** Hourly, session-bound random 15-minute block for user-installed apps. */
public final class RandomFullBlock {
    private static final long HOUR = 3600000L;
    private static final long WINDOW = 900000L;
    private static final SecureRandom RNG = new SecureRandom();
    private static String sessionId = "";
    private static long[] starts = new long[0];
    private static long lastElapsed = -1L;
    private static Context appContext;
    private RandomFullBlock() {}

    private static Object call(Object target, String name) throws Exception {
        Method m = target.getClass().getMethod(name);
        return m.invoke(target);
    }

    private static Object sessionRepository() throws Exception {
        Class<?> graph = Class.forName("com.v1f.app.AppGraph");
        Object instance = graph.getField("INSTANCE").get(null);
        return call(instance, "getSessionRepository");
    }

    private static synchronized boolean windowActive(Context context, long elapsed, String id) {
        appContext = context.getApplicationContext();
        if (!id.equals(sessionId) || (lastElapsed >= 0 && elapsed + 120000L < lastElapsed)) {
            sessionId = id;
            starts = new long[0];
        }
        lastElapsed = elapsed;
        long index = elapsed / HOUR;
        if (index < 0 || index >= 100000) return false;
        if (index >= starts.length) {
            int required = (int) index + 1;
            long[] expanded = new long[required];
            System.arraycopy(starts, 0, expanded, 0, starts.length);
            for (int i = starts.length; i < required; i++) {
                // Minute 1..45 inclusive: 15 minutes finish by the hour boundary.
                android.content.SharedPreferences prefs = appContext.getSharedPreferences("vx_random_full_block", Context.MODE_PRIVATE);
                String key = id + ":hour:" + i;
                int minute = prefs.getInt(key, -1);
                if (minute < 1 || minute > 45) {
                    minute = 1 + RNG.nextInt(45);
                    prefs.edit().putInt(key, minute).apply();
                }
                expanded[i] = minute * 60000L;
            }
            starts = expanded;
        }
        long minuteInHour = elapsed % HOUR;
        return minuteInHour >= starts[(int)index] && minuteInHour < starts[(int)index] + WINDOW;
    }

    public static boolean shouldBlock(AccessibilityService service, AccessibilityEvent event) {
        if (service == null || event == null || event.getPackageName() == null) return false;
        int type = event.getEventType();
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
                type != AccessibilityEvent.TYPE_WINDOWS_CHANGED) return false;
        String pkg = event.getPackageName().toString();
        if (pkg.equals(service.getPackageName()) || pkg.equals("com.android.dialer") ||
                pkg.equals("com.google.android.dialer") || pkg.equals("com.android.phone") ||
                pkg.equals("com.android.server.telecom") || pkg.equals("com.android.incallui") ||
                pkg.equals("com.google.android.apps.dialer")) return false;
        try {
            Object repository = sessionRepository();
            Object session = call(call(repository, "getSession"), "getValue");
            if (session == null) return false;
            long duration = ((Number)call(session, "getDurationMillis")).longValue();
            if (duration < 24L * HOUR) return false;
            long remaining = ((Number)call(repository, "remainingMillis")).longValue();
            if (remaining <= 0 || remaining > duration) return false;
            // The existing break runtime is authoritative: never override its free minute.
            if (StrictBlockPolicy.allowsAll()) return false;
            String id = String.valueOf(call(session, "getId"));
            long elapsed = duration - remaining;
            if (!windowActive(service, elapsed, id)) return false;
            PackageManager pm = service.getPackageManager();
            ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
            // Only apps installed by the user, including updated system apps.
            if ((info.flags & ApplicationInfo.FLAG_SYSTEM) != 0 &&
                    (info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0) return false;
            // Never block the active default phone app, even if user-installed.
            android.content.Intent dial = new android.content.Intent(android.content.Intent.ACTION_DIAL);
            android.content.pm.ResolveInfo phone = pm.resolveActivity(dial, PackageManager.MATCH_DEFAULT_ONLY);
            if (phone != null && phone.activityInfo != null &&
                    pkg.equals(phone.activityInfo.packageName)) return false;
            return true;
        } catch (Throwable ignored) {
            return false; // Keep the original selective blocking when introspection fails.
        }
    }

    public static String status(Context context) {
        try {
            Object repository = sessionRepository();
            Object session = call(call(repository, "getSession"), "getValue");
            if (session == null) return "بانتظار بدء جلسة حظر";
            long duration = ((Number)call(session, "getDurationMillis")).longValue();
            if (duration < 24L * HOUR) return "يتفعّل تلقائياً عند حظر 24 ساعة أو أكثر";
            long remaining = ((Number)call(repository, "remainingMillis")).longValue();
            if (remaining <= 0) return "بانتظار بدء جلسة حظر";
            if (StrictBlockPolicy.allowsAll()) return "دقيقة الاستراحة — الحظر الشامل متوقف مؤقتاً";
            String id = String.valueOf(call(session, "getId"));
            return windowActive(context, duration - remaining, id) ?
                    "● الحظر الشامل نشط الآن (15 دقيقة)" :
                    "● النظام العشوائي يعمل — موعد الحظر مخفي";
        } catch (Throwable ignored) {
            return "يتفعّل تلقائياً عند حظر 24 ساعة أو أكثر";
        }
    }
}
