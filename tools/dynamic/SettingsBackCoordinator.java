package com.v1f.app.patch;

import android.accessibilityservice.AccessibilityService;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import com.v1f.app.protection.overlay.MeteorShieldOverlay;

/**
 * Step 1: issue two BACK actions immediately on a blocked Settings click.
 * Do not check the foreground package, window, or page before either action.
 * Future stages of the escape policy will be added separately.
 */
public final class SettingsBackCoordinator {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final long SHIELD_DURATION_MS = 782L;
    private static final long DUPLICATE_EVENT_WINDOW_MS = 120L;
    private static long lastTriggerMs;
    private static MeteorShieldOverlay shield;
    private static Runnable hideTask;
    private static int generation;

    private SettingsBackCoordinator() {}

    public static void trigger(final AccessibilityService service,
                               final MeteorShieldOverlay overlay) {
        if (service == null) return;
        if (Looper.myLooper() != Looper.getMainLooper()) {
            MAIN.post(() -> trigger(service, overlay));
            return;
        }
        long now = SystemClock.elapsedRealtime();
        if (lastTriggerMs != 0L && now - lastTriggerMs < DUPLICATE_EVENT_WINDOW_MS) {
            return;
        }
        lastTriggerMs = now;

        // Two consecutive BACK requests. No foreground checks or delay.
        // Do not let an OS rejection of the first action prevent the second
        // action or the return monitor from starting. No checks or delays.
        try { service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK); }
        catch (RuntimeException ignored) {}
        try { service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK); }
        catch (RuntimeException ignored) {}

        // Arm the independent return observer only AFTER initial BACK x2.
        SettingsReturnEngine.arm(service);

        // Show the existing block overlay only after both BACK requests.
        shield = overlay;
        showShield();
    }

    // Retained for compatibility with the service hook; later steps will use it.
    public static boolean observe(AccessibilityService service, AccessibilityEvent event) {
        return SettingsReturnEngine.observe(service, event);
    }

    private static void showShield() {
        if (shield == null) return;
        try { shield.show(); } catch (RuntimeException ignored) {}
        if (hideTask != null) MAIN.removeCallbacks(hideTask);
        final int token = ++generation;
        hideTask = () -> {
            if (generation != token) return;
            try { if (shield != null) shield.hide(); }
            catch (RuntimeException ignored) {}
            hideTask = null;
        };
        MAIN.postDelayed(hideTask, SHIELD_DURATION_MS);
    }

    /** Used only for OS service teardown: do NOT discard the return request. */
    public static void detach() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            MAIN.post(SettingsBackCoordinator::detach);
            return;
        }
        SettingsReturnEngine.detach();
        generation++;
        if (hideTask != null) MAIN.removeCallbacks(hideTask);
        hideTask = null;
        if (shield != null) {
            try { shield.hide(); } catch (RuntimeException ignored) {}
        }
        shield = null;
        lastTriggerMs = 0L;
    }

    public static void cancel() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            MAIN.post(SettingsBackCoordinator::cancel);
            return;
        }
        SettingsReturnEngine.cancel();
        generation++;
        if (hideTask != null) MAIN.removeCallbacks(hideTask);
        hideTask = null;
        if (shield != null) {
            try { shield.hide(); } catch (RuntimeException ignored) {}
        }
        shield = null;
        lastTriggerMs = 0L;
    }
}
