package com.v1f.app.patch;

import android.accessibilityservice.AccessibilityService;
import android.os.Handler;
import android.os.Looper;
import java.lang.reflect.Method;

/**
 * Only for stock app/site block decisions, NEVER for Settings interception.
 *
 * On a detected app or blocked website, send five system BACK actions spaced
 * 250ms apart, then invoke the ORIGINAL showApplicationLock/showWebsiteLock
 * to show the unchanged LockActivity.  No app relaunch, no window scanning
 * and no polling.  GuardBridge's existing grace runs before this hook.
 */
public final class BlockedBackCoordinator {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final String SETTINGS = "com.android.settings";

    private static AccessibilityService service;
    private static String blockedPackage;
    private static String blockedDomain;
    private static Runnable pendingTask;
    private static int generation;
    private static boolean active;
    private static boolean replaying;

    private BlockedBackCoordinator() {}

    public static boolean interceptApp(AccessibilityService current, String packageName) {
        return intercept(current, packageName, null);
    }

    /** For a blocked website, packageName is the ACTUAL browser package. */
    public static boolean interceptWebsite(AccessibilityService current, String domain,
                                           String packageName) {
        if (domain == null || domain.isEmpty()) return false;
        return intercept(current, packageName, domain);
    }

    private static boolean intercept(AccessibilityService current, String packageName,
                                     String domain) {
        if (current == null || packageName == null || packageName.isEmpty()) return false;
        // Settings click/exit/overlay enforcement is completely separate.
        if (SETTINGS.equals(packageName) || packageName.equals(current.getPackageName()))
            return false;
        if (!SettingsSessionGate.isActive() || StrictBlockPolicy.allowsAll()) {
            cancel();
            return false;
        }
        // The original lock method re-enters this hook via reflection only once.
        if (replaying) return false;
        // BACK-driven window events must not start nested five-BACK sequences.
        if (active) return true;
        // Android dispatches AccessibilityService events on the main looper.
        // If invoked off-main, retain the original blocking path.
        if (Looper.myLooper() != Looper.getMainLooper()) return false;

        active = true;
        service = current;
        blockedPackage = packageName;
        blockedDomain = domain;
        final int token = ++generation;
        performStep(token, 0);
        return true;
    }

    private static void performStep(final int token, final int step) {
        if (token != generation || !active || service == null) return;
        if (!SettingsSessionGate.isActive() || StrictBlockPolicy.allowsAll()) {
            cancel();
            return;
        }

        if (step < BlockedBackSequencePlan.BACK_COUNT) {
            try {
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
            } catch (Throwable ignored) {
                // Continue all requested attempts, then retain the original lock.
            }
            Runnable next = new Runnable() {
                @Override public void run() { performStep(token, step + 1); }
            };
            pendingTask = next;
            if (!MAIN.postDelayed(next, BlockedBackSequencePlan.INTERVAL_MS)) {
                pendingTask = null;
                showOriginalLock(token);
            }
        } else {
            showOriginalLock(token);
        }
    }

    private static void showOriginalLock(int token) {
        if (token != generation || !active || service == null) return;
        if (!SettingsSessionGate.isActive() || StrictBlockPolicy.allowsAll()) {
            cancel();
            return;
        }
        AccessibilityService current = service;
        String pkg = blockedPackage;
        String domain = blockedDomain;
        replaying = true;
        try {
            if (domain == null) {
                Method original = current.getClass().getDeclaredMethod(
                        "showApplicationLock", String.class);
                original.setAccessible(true);
                original.invoke(current, pkg);
            } else {
                Method original = current.getClass().getDeclaredMethod(
                        "showWebsiteLock", String.class, String.class);
                original.setAccessible(true);
                original.invoke(current, domain, pkg);
            }
        } catch (Throwable ignored) {
            // Failed reflection will not trigger an endless series of BACKs.
        } finally {
            replaying = false;
            clear();
        }
    }

    private static void clear() {
        generation++;
        if (pendingTask != null) MAIN.removeCallbacks(pendingTask);
        pendingTask = null;
        service = null;
        blockedPackage = null;
        blockedDomain = null;
        active = false;
    }

    /** Cancel on accessibility service pause/destroy or whenever the break starts. */
    public static void cancel() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            MAIN.post(BlockedBackCoordinator::cancel);
            return;
        }
        clear();
    }
}
