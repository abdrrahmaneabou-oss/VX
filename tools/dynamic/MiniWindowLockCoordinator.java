package com.v1f.app.patch;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

/**
 * Bring a blocked app (or the browser showing a blocked site) out of Mini mode
 * BEFORE dispatching the existing LockActivity route. Do not open site URLs.
 *
 * Single-flight + generation checks prevent our own launch accessibility events
 * from recursively scheduling launches. Foreground mini windows are also polled
 * so tapping the OEM Mini control while LockActivity is visible is covered.
 */
public final class MiniWindowLockCoordinator {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final long RELOCK_DELAY_MS = 450L;
    private static final long POLL_MS = 400L;
    private static final long RETRY_SPACING_MS = 1200L;
    private static final long BROWSER_HINT_MS = 30000L;
    private static final String SETTINGS = "com.android.settings";

    private static AccessibilityService attached;
    private static Runnable poll;
    private static Runnable replay;
    private static int generation;
    private static boolean callingOriginal;
    private static String pendingPackage;
    private static String pendingDomain;
    private static long lastLaunchAt;
    private static String lastBrowser;
    private static String lastDomain;
    private static long lastWebsiteDetectedAt;

    private MiniWindowLockCoordinator() {}

    /** Called BEFORE the base APK's showApplicationLock body. */
    public static boolean interceptApp(AccessibilityService service, String targetPackage) {
        return intercept(service, targetPackage, null);
    }

    /** Called BEFORE showWebsiteLock(domain, browserPackage): launch the BROWSER only. */
    public static boolean interceptWebsite(AccessibilityService service, String domain,
                                           String browserPackage) {
        if (service != null && attached != service) attach(service);
        if (SettingsSessionGate.isActive() && !StrictBlockPolicy.allowsAll()
                && domain != null && browserPackage != null) {
            lastBrowser = browserPackage;
            lastDomain = domain;
            lastWebsiteDetectedAt = SystemClock.elapsedRealtime();
        }
        return intercept(service, browserPackage, domain);
    }

    private static boolean intercept(AccessibilityService service, String target, String domain) {
        if (service == null || target == null || target.length() == 0) return false;
        if (!SettingsSessionGate.isActive() || StrictBlockPolicy.allowsAll()) {
            clearPending();
            return false;
        }
        if (callingOriginal || target.equals(service.getPackageName())
                || SETTINGS.equals(target)) return false;
        if (attached != service) attach(service);
        if (replay != null && target.equals(pendingPackage)
                && equal(domain, pendingDomain)) return true;
        if (Looper.myLooper() != Looper.getMainLooper()) return false;
        // Normal full-screen blocking must retain the original immediate path.
        // Only OEM Mini windows need a new foreground launch before LockActivity.
        if (!hasMiniWindow(service, target)) return false;
        // Ignore our own launch events while the phone is switching task modes.
        // The scheduled replay already delivers the actual lock.
        if (SystemClock.elapsedRealtime() - lastLaunchAt < RETRY_SPACING_MS) return true;
        cancelReplay();
        return launchThenLock(service, target, domain);
    }

    private static boolean launchThenLock(final AccessibilityService service,
                                          final String pkg, final String domain) {
        final Intent launch;
        try {
            launch = service.getPackageManager().getLaunchIntentForPackage(pkg);
            if (launch == null) return false; // Stock lock path remains available.
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        } catch (RuntimeException ignored) {
            return false;
        }
        final int token = ++generation;
        pendingPackage = pkg;
        pendingDomain = domain;
        replay = () -> {
            if (token != generation || attached != service) return;
            replay = null;
            pendingPackage = null;
            pendingDomain = null;
            if (!SettingsSessionGate.isActive() || StrictBlockPolicy.allowsAll()) return;
            callingOriginal = true;
            try {
                Method original = service.getClass().getDeclaredMethod(
                        domain == null ? "showApplicationLock" : "showWebsiteLock",
                        domain == null ? new Class<?>[]{String.class}
                                       : new Class<?>[]{String.class, String.class});
                original.setAccessible(true);
                if (domain == null) original.invoke(service, pkg);
                else original.invoke(service, domain, pkg);
            } catch (Throwable ignored) {
                // Failure cannot cause an endless relaunch; a new detection retries.
            } finally {
                callingOriginal = false;
            }
        };
        try {
            lastLaunchAt = SystemClock.elapsedRealtime();
            service.startActivity(launch);
            MAIN.postDelayed(replay, RELOCK_DELAY_MS);
            return true;
        } catch (RuntimeException ignored) {
            cancelReplay();
            return false;
        }
    }

    public static void attach(AccessibilityService service) {
        if (service == null) return;
        if (Looper.myLooper() != Looper.getMainLooper()) {
            MAIN.post(() -> attach(service));
            return;
        }
        if (attached == service && poll != null) return;
        detach();
        attached = service;
        poll = new Runnable() {
            @Override public void run() {
                if (attached != service) return;
                if (!SettingsSessionGate.isActive() || StrictBlockPolicy.allowsAll()) {
                    cancelReplay();
                    if (!SettingsSessionGate.isActive()) clearWebsite();
                } else if (replay == null
                        && SystemClock.elapsedRealtime() - lastLaunchAt >= RETRY_SPACING_MS) {
                    inspectMiniWindows(service);
                }
                if (attached == service && poll == this) MAIN.postDelayed(this, POLL_MS);
            }
        };
        MAIN.postDelayed(poll, POLL_MS);
    }

    /** This follows the actual accessibility TYPE_APPLICATION window bounds. */
    private static void inspectMiniWindows(AccessibilityService service) {
        final Point screen = new Point();
        try {
            WindowManager manager = (WindowManager) service.getSystemService(Context.WINDOW_SERVICE);
            if (manager == null) return;
            manager.getDefaultDisplay().getRealSize(screen);
        } catch (RuntimeException ignored) { return; }
        if (screen.x <= 0 || screen.y <= 0) return;

        List<AccessibilityWindowInfo> windows;
        try { windows = service.getWindows(); } catch (RuntimeException ignored) { return; }
        if (windows == null) return;
        for (AccessibilityWindowInfo window : windows) {
            if (window == null || window.getType() != AccessibilityWindowInfo.TYPE_APPLICATION) continue;
            AccessibilityNodeInfo root = null;
            try {
                root = window.getRoot();
                if (root == null || root.getPackageName() == null) continue;
                String pkg = root.getPackageName().toString();
                if (pkg.equals(service.getPackageName()) || SETTINGS.equals(pkg)) continue;
                Rect bounds = new Rect();
                window.getBoundsInScreen(bounds);
                if (!MiniWindowRules.isMiniBounds(bounds.width(), bounds.height(), screen.x, screen.y)) continue;
                if (blockedApp(service, pkg)) {
                    if (launchThenLock(service, pkg, null)) return;
                } else if (pkg.equals(lastBrowser) && lastDomain != null
                        && SystemClock.elapsedRealtime() - lastWebsiteDetectedAt < BROWSER_HINT_MS) {
                    // Only browsers recently identified by the actual WebsiteGuard.
                    // Never block every window of a browser just because it exists.
                    if (launchThenLock(service, pkg, lastDomain)) return;
                }
            } catch (Throwable ignored) {
            } finally {
                if (root != null) try { root.recycle(); } catch (RuntimeException ignored) {}
            }
        }
    }

    private static boolean hasMiniWindow(AccessibilityService service, String target) {
        Point screen = new Point();
        try {
            WindowManager manager = (WindowManager) service.getSystemService(Context.WINDOW_SERVICE);
            if (manager == null) return false;
            manager.getDefaultDisplay().getRealSize(screen);
            if (screen.x <= 0 || screen.y <= 0) return false;
            List<AccessibilityWindowInfo> windows = service.getWindows();
            if (windows == null) return false;
            for (AccessibilityWindowInfo window : windows) {
                if (window == null || window.getType() != AccessibilityWindowInfo.TYPE_APPLICATION) continue;
                AccessibilityNodeInfo node = null;
                try {
                    node = window.getRoot();
                    if (node == null || node.getPackageName() == null
                            || !target.equals(node.getPackageName().toString())) continue;
                    Rect bounds = new Rect();
                    window.getBoundsInScreen(bounds);
                    if (MiniWindowRules.isMiniBounds(
                            bounds.width(), bounds.height(), screen.x, screen.y)) return true;
                } finally {
                    if (node != null) node.recycle();
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private static boolean blockedApp(Context context, String pkg) {
        try {
            Class<?> graphClass = Class.forName("com.v1f.app.AppGraph");
            Object graph = graphClass.getField("INSTANCE").get(null);
            Object repo = graphClass.getMethod("getSessionRepository").invoke(graph);
            Object flow = repo.getClass().getMethod("getSession").invoke(repo);
            Object session = flow.getClass().getMethod("getValue").invoke(flow);
            if (session == null) return false;
            Object blocked = session.getClass().getMethod("getBlockedPackages").invoke(session);
            if (blocked instanceof Set && ((Set<?>) blocked).contains(pkg)) return true;
            return RandomFullBlock.remainingMillis(context, pkg) > 0L;
        } catch (Throwable ignored) {
            return RandomFullBlock.remainingMillis(context, pkg) > 0L;
        }
    }

    private static boolean equal(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static void cancelReplay() {
        generation++;
        if (replay != null) MAIN.removeCallbacks(replay);
        replay = null;
        pendingPackage = null;
        pendingDomain = null;
    }

    private static void clearWebsite() {
        lastBrowser = null;
        lastDomain = null;
        lastWebsiteDetectedAt = 0L;
    }

    private static void clearPending() {
        cancelReplay();
        clearWebsite();
    }

    public static void detach() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            MAIN.post(MiniWindowLockCoordinator::detach);
            return;
        }
        cancelReplay();
        if (poll != null) MAIN.removeCallbacks(poll);
        poll = null;
        attached = null;
        clearWebsite();
    }
}
