package com.v1f.app.patch;

import android.accessibilityservice.AccessibilityService;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import com.v1f.app.protection.overlay.MeteorShieldOverlay;

/**
 * Home-safe Settings escape. First BACK is immediate; BACK #2 and #3 only run
 * with Settings in the active accessibility window. Pending actions survive
 * Home until Settings returns, without Shizuku or any accessibility polling.
 */
public final class SettingsBackCoordinator {
    private static final String SETTINGS = "com.android.settings";
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());
    private static final long THIRD_DELAY_MS = 100L;
    private static final long SECOND_DELAY_MS = 24L;
    private static final long SHIELD_DURATION_MS = 782L;
    private static final long DEBOUNCE_MS = 900L;
    private static AccessibilityService service;
    private static MeteorShieldOverlay shield;
    private static int remaining;
    private static int generation;
    private static long lastTrigger;
    private static long lastAccepted;
    private static Runnable nextBack;
    private static Runnable hideShield;

    private SettingsBackCoordinator() {}

    public static void trigger(AccessibilityService current, MeteorShieldOverlay overlay) {
        if (current == null || overlay == null) return;
        if (Looper.myLooper() != Looper.getMainLooper()) {
            HANDLER.post(() -> trigger(current, overlay));
            return;
        }
        long now = SystemClock.elapsedRealtime();
        if (remaining > 0 || (lastTrigger > 0 && now - lastTrigger < DEBOUNCE_MS))
            return;
        generation++;
        service = current;
        shield = overlay;
        lastTrigger = now;
        remaining = 3;

        // The user explicitly requested an unconditional immediate first BACK.
        if (sendBack()) {
            remaining--;
            lastAccepted = now;
        }

        showShield();
        schedule(SECOND_DELAY_MS);
    }

    public static void observe(AccessibilityService current, AccessibilityEvent event) {
        if (current == null || event == null || remaining <= 0) return;
        if (Looper.myLooper() != Looper.getMainLooper()) {
            HANDLER.post(() -> observe(current, event));
            return;
        }
        int type = event.getEventType();
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && type != AccessibilityEvent.TYPE_WINDOWS_CHANGED
                && type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                && type != AccessibilityEvent.TYPE_VIEW_CLICKED)
            return;
        service = current;
        if (settingsForeground()) schedule(0);
    }

    private static boolean settingsForeground() {
        if (service == null) return false;
        try {
            AccessibilityNodeInfo root = service.getRootInActiveWindow();
            return root != null && root.getPackageName() != null
                    && SETTINGS.contentEquals(root.getPackageName());
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static boolean sendBack() {
        if (service == null) return false;
        try {
            return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static void schedule(long delay) {
        if (remaining == 0 || service == null) return;
        if (nextBack != null) HANDLER.removeCallbacks(nextBack);
        final int token = generation;
        nextBack = () -> {
            if (generation != token) return;
            nextBack = null;
            advance();
        };
        HANDLER.postDelayed(nextBack, Math.max(0, delay));
    }

    private static void advance() {
        if (remaining == 0) return;
        // Never direct a delayed BACK at Home, an unrelated app, or a lock screen.
        // No timer expires the pending transaction while Settings is backgrounded.
        if (!settingsForeground()) return;
        if (remaining == 1) {
            long wait = THIRD_DELAY_MS - (SystemClock.elapsedRealtime() - lastAccepted);
            if (wait > 0) {
                schedule(wait);
                return;
            }
        }
        if (sendBack()) {
            remaining--;
            lastAccepted = SystemClock.elapsedRealtime();
            if (remaining > 0)
                schedule(remaining == 1 ? THIRD_DELAY_MS : SECOND_DELAY_MS);
        }
        // Rejected actions remain queued until another qualifying event.
    }

    private static void showShield() {
        if (shield == null) return;
        try { shield.show(); } catch (RuntimeException ignored) {}
        if (hideShield != null) HANDLER.removeCallbacks(hideShield);
        final int token = generation;
        hideShield = () -> {
            if (token != generation) return;
            try { shield.hide(); } catch (RuntimeException ignored) {}
            hideShield = null;
        };
        HANDLER.postDelayed(hideShield, SHIELD_DURATION_MS);
    }

    public static void cancel() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            HANDLER.post(SettingsBackCoordinator::cancel);
            return;
        }
        generation++;
        remaining = 0;
        if (nextBack != null) HANDLER.removeCallbacks(nextBack);
        if (hideShield != null) HANDLER.removeCallbacks(hideShield);
        nextBack = null;
        hideShield = null;
        if (shield != null) {
            try { shield.hide(); } catch (RuntimeException ignored) {}
        }
        service = null;
        shield = null;
    }
}
