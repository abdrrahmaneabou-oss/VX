package com.v1f.app.patch;

import android.accessibilityservice.AccessibilityService;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.lang.reflect.Method;
import java.util.Set;

/** Only GuardBridge gets a fresh 15-second grace period per foreground opening. */
public final class GuardBridgeGrace {
    private static final String TARGET = "com.anas.guardbridge";
    private static final long DELAY_MS = 15000L;
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());
    private static long openedAt = -1L;
    private static boolean foreground = false;
    private static Runnable pending;
    private static AccessibilityService activeService;

    private GuardBridgeGrace() {}

    public static synchronized void onEvent(AccessibilityService service, AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;
        int type = event.getEventType();
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            type != AccessibilityEvent.TYPE_WINDOWS_CHANGED) return;
        String pkg = event.getPackageName().toString();
        if (TARGET.equals(pkg)) {
            if (!foreground || activeService != service) {
                cancel();
                foreground = true;
                activeService = service;
                openedAt = SystemClock.elapsedRealtime();
            }
        } else if (foreground && !pkg.equals(service.getPackageName()) &&
                   !pkg.equals("com.android.systemui")) {
            cancel();
        }
    }

    private static void cancel() {
        if (pending != null) HANDLER.removeCallbacks(pending);
        pending = null;
        foreground = false;
        openedAt = -1L;
        activeService = null;
    }

    private static boolean isSelectedAndActive() {
        try {
            Class<?> graphClass = Class.forName("com.v1f.app.AppGraph");
            Object graph = graphClass.getField("INSTANCE").get(null);
            Object repo = graphClass.getMethod("getSessionRepository").invoke(graph);
            Object flow = repo.getClass().getMethod("getSession").invoke(repo);
            Object session = flow.getClass().getMethod("getValue").invoke(flow);
            if (session == null) return false;
            Object selected = session.getClass().getMethod("getBlockedPackages").invoke(session);
            long remaining = ((Number)repo.getClass().getMethod("remainingMillis").invoke(repo)).longValue();
            return remaining > 0 && selected instanceof Set &&
                   ((Set<?>) selected).contains(TARGET) && !StrictBlockPolicy.allowsAll();
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Called at the single app-lock entry point, covering normal and random blocking. */
    public static synchronized boolean shouldDelay(AccessibilityService service, String pkg) {
        if (!TARGET.equals(pkg)) return false;
        if (!isSelectedAndActive()) {
            cancel();
            return false;
        }
        if (!foreground || activeService != service) {
            cancel();
            foreground = true;
            activeService = service;
            openedAt = SystemClock.elapsedRealtime();
        }
        long elapsed = SystemClock.elapsedRealtime() - openedAt;
        if (elapsed >= DELAY_MS) return false;
        if (pending == null) {
            final AccessibilityService current = service;
            pending = new Runnable() {
                @Override public void run() {
                    synchronized (GuardBridgeGrace.class) {
                        pending = null;
                        if (!foreground || activeService != current ||
                            SystemClock.elapsedRealtime() - openedAt < DELAY_MS ||
                            !isSelectedAndActive()) return;
                        try {
                            AccessibilityNodeInfo root = current.getRootInActiveWindow();
                            if (root == null || root.getPackageName() == null ||
                                !TARGET.equals(root.getPackageName().toString())) {
                                cancel();
                                return;
                            }
                            Method m = current.getClass().getDeclaredMethod("showApplicationLock", String.class);
                            m.setAccessible(true);
                            m.invoke(current, TARGET);
                        } catch (Throwable ignored) {}
                    }
                }
            };
            HANDLER.postDelayed(pending, DELAY_MS - elapsed);
        }
        return true;
    }

    public static synchronized void detach(AccessibilityService service) {
        if (activeService == service) cancel();
    }
}
