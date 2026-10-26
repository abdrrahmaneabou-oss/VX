package com.v1f.app.patch;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.accessibility.AccessibilityEvent;
import java.lang.reflect.Method;


/** Session-bound block in the final minutes of each local clock hour. */
public final class RandomFullBlock {
    private static final long HOUR = 3600000L;
    private static final int DEFAULT_MINUTES = 1;
    private static final String PREFS = "vx_random_full_block";
    private static final String KEY_MINUTES = "window_minutes";
    private static android.os.Handler pollHandler;
    private static Runnable pollTask;
    private static long lastForcedAt;
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

    public static int getMinutes(Context context) {
        return Math.max(1, Math.min(45, context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_MINUTES, DEFAULT_MINUTES)));
    }

    public static int adjustMinutes(Context context, int delta) {
        if (!StrictBlockPolicy.allowsAll()) return getMinutes(context);
        int next = Math.max(1, Math.min(45, getMinutes(context) + delta));
        context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY_MINUTES, next).apply();
        return next;
    }

    private static boolean windowActive(Context context) {
        return FullBlockRules.windowActive(System.currentTimeMillis(),
                java.util.TimeZone.getDefault(), getMinutes(context));
    }

    public static boolean shouldBlock(AccessibilityService service, AccessibilityEvent event) {
        if (service == null || event == null || event.getPackageName() == null) return false;
        int type = event.getEventType();
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
                type != AccessibilityEvent.TYPE_WINDOWS_CHANGED) return false;
        return shouldBlockPackage(service, event.getPackageName().toString());
    }

    private static boolean shouldBlockPackage(AccessibilityService service, String pkg) {
        return remainingMillis(service, pkg) > 0;
    }

    /** Shared eligibility for enforcement and the lock screen; zero means no hourly block. */
    public static long remainingMillis(Context context, String pkg) {
        try {
            if (pkg == null || FullBlockSettings.isExempt(context, pkg)) return 0;
            Object repository = sessionRepository();
            Object session = call(call(repository, "getSession"), "getValue");
            if (session == null) return 0;
            long duration = ((Number)call(session, "getDurationMillis")).longValue();
            if (duration < 24L * HOUR) return 0;
            long remaining = ((Number)call(repository, "remainingMillis")).longValue();
            if (remaining <= 0 || remaining > duration) return 0;
            // The existing break runtime is authoritative: never override its free minute.
            if (StrictBlockPolicy.allowsAll()) return 0;
            long hourlyRemaining = FullBlockRules.remainingMillis(System.currentTimeMillis(),
                    java.util.TimeZone.getDefault(), getMinutes(context));
            if (hourlyRemaining <= 0 || !eligiblePackage(context, pkg)) return 0;
            return Math.min(hourlyRemaining, remaining);
        } catch (Throwable ignored) {
            return 0; // Keep the original selective blocking when introspection fails.
        }
    }

    public static boolean eligiblePackage(Context context, String pkg) {
        if (pkg == null) return false;
        if (pkg.equals(context.getPackageName()) || pkg.equals("com.android.dialer") ||
                pkg.equals("com.google.android.dialer") || pkg.equals("com.android.phone") ||
                pkg.equals("com.android.server.telecom") || pkg.equals("com.android.incallui") ||
                pkg.equals("com.google.android.apps.dialer")) return false;
        try {
            PackageManager pm = context.getPackageManager();
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
        } catch (PackageManager.NameNotFoundException ignored) {
            return false;
        }
    }

    /** Catch apps already foreground when the hourly window starts. */
    public static synchronized void attach(final AccessibilityService service) {
        detach(service);
        pollHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        pollTask = new Runnable() {
            @Override public void run() {
                try {
                    android.view.accessibility.AccessibilityNodeInfo root = service.getRootInActiveWindow();
                    if (root != null && root.getPackageName() != null) {
                        String pkg = root.getPackageName().toString();
                        if (shouldBlockPackage(service, pkg)) {
                            long now = android.os.SystemClock.elapsedRealtime();
                            if (now - lastForcedAt >= 5000L) {
                                lastForcedAt = now;
                                Method m = service.getClass().getDeclaredMethod("showApplicationLock", String.class);
                                m.setAccessible(true);
                                m.invoke(service, pkg);
                            }
                        }
                    }
                } catch (Throwable ignored) {}
                synchronized (RandomFullBlock.class) {
                    if (pollTask == this && pollHandler != null)
                        pollHandler.postDelayed(this, 2000L);
                }
            }
        };
        pollHandler.postDelayed(pollTask, 2000L);
    }

    public static synchronized void detach(AccessibilityService service) {
        if (pollHandler != null && pollTask != null) pollHandler.removeCallbacks(pollTask);
        pollTask = null;
        pollHandler = null;
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
            return windowActive(context) ?
                    "● الحظر الشامل نشط الآن (" + getMinutes(context) + " دقيقة)" :
                    "● الحظر من الدقيقة " + (60 - getMinutes(context)) + " حتى بداية الساعة التالية";
        } catch (Throwable ignored) {
            return "يتفعّل تلقائياً عند حظر 24 ساعة أو أكثر";
        }
    }
}
