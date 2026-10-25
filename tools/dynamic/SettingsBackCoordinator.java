package com.v1f.app.patch;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.Toast;
import com.v1f.app.protection.overlay.MeteorShieldOverlay;

/** A single shield -> Settings foreground -> BACK -> 300ms -> BACK transaction. */
public final class SettingsBackCoordinator {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final String SETTINGS = "com.android.settings";
    private static AccessibilityService service;
    private static MeteorShieldOverlay shield;
    private static Runnable pending;
    private static int generation;
    private static int stage; // 0 idle, 1 waiting for Settings, 2 BACK sequence
    private static long launchedAt;
    private static long quietUntil;
    private static int focusedSamples;

    private SettingsBackCoordinator() {}

    public static void initialize(AccessibilityService current) {
        // Keep window delivery configuration, but retire the old return BACK pair.
        SettingsReturnEngine.initialize(current);
        SettingsReturnEngine.cancel();
    }

    public static void trigger(final AccessibilityService current,
                               final MeteorShieldOverlay overlay) {
        if (current == null) return;
        if (Looper.myLooper() != Looper.getMainLooper()) {
            MAIN.post(() -> trigger(current, overlay));
            return;
        }
        if (stage != 0 || SystemClock.elapsedRealtime() < quietUntil) return;
        SettingsReturnEngine.cancel();
        service = current;
        shield = overlay;
        generation++;
        stage = 1;
        focusedSamples = 0;
        try {
            if (shield == null || !shield.show()) {
                fail("Could not show protection shield");
                return;
            }
        } catch (RuntimeException error) {
            fail("Could not show protection shield");
            return;
        }
        // Post launch so the shield gets a chance to attach first.
        schedule(() -> {
            launchedAt = SystemClock.uptimeMillis();
            try {
                Intent intent = current.getPackageManager().getLaunchIntentForPackage(SETTINGS);
                if (intent == null) intent = new Intent(Settings.ACTION_SETTINGS).setPackage(SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                current.startActivity(intent);
                pollForeground();
            } catch (RuntimeException error) {
                fail("Could not open Settings");
            }
        }, 0L);
    }

    public static boolean observe(AccessibilityService current, AccessibilityEvent event) {
        if (stage == 0) return SystemClock.elapsedRealtime() < quietUntil;
        // Consume events during our own navigation to prevent recursive blocking.
        if (stage == 1 && launchedAt != 0L && event != null
                && event.getEventTime() >= launchedAt
                && event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && SETTINGS.contentEquals(event.getPackageName() == null ? "" : event.getPackageName())) {
            beginBacks();
        }
        return true;
    }

    private static boolean settingsFocused() {
        try {
            for (AccessibilityWindowInfo window : service.getWindows()) {
                if (window.getType() != AccessibilityWindowInfo.TYPE_APPLICATION
                        || !window.isFocused()) continue;
                AccessibilityNodeInfo root = window.getRoot();
                if (root != null) {
                    try { return SETTINGS.contentEquals(root.getPackageName() == null ? "" : root.getPackageName()); }
                    finally { root.recycle(); }
                }
            }
        } catch (RuntimeException ignored) {}
        return false;
    }

    private static void pollForeground() {
        if (stage != 1) return;
        // Handles an already foreground Settings task without trusting a stale root.
        if (SystemClock.uptimeMillis() - launchedAt >= 300L && settingsFocused()) {
            if (++focusedSamples >= 2) {
                beginBacks();
                return;
            }
        } else focusedSamples = 0;
        // No five-second cutoff: the shield stays until the operation finishes.
        schedule(SettingsBackCoordinator::pollForeground, 100L);
    }

    private static void beginBacks() {
        if (stage != 1) return;
        stage = 2;
        removePending();
        sendBack(1);
    }

    private static void sendBack(final int number) {
        if (stage != 2 || service == null) return;
        boolean accepted = false;
        try { accepted = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK); }
        catch (RuntimeException ignored) {}
        if (!accepted) {
            schedule(() -> sendBack(number), 150L);
        } else if (number == 1) {
            schedule(() -> sendBack(2), 300L);
        } else {
            finish();
        }
    }

    private static void schedule(Runnable action, long delay) {
        removePending();
        final int token = generation;
        pending = () -> {
            if (token != generation) return;
            pending = null;
            action.run();
        };
        MAIN.postDelayed(pending, delay);
    }

    private static void removePending() {
        if (pending != null) MAIN.removeCallbacks(pending);
        pending = null;
    }

    private static void finish() {
        generation++;
        removePending();
        stage = 0;
        launchedAt = 0L;
        quietUntil = SystemClock.elapsedRealtime() + 700L;
        if (shield != null) {
            try { shield.hide(); } catch (RuntimeException ignored) {}
        }
        shield = null;
        service = null;
    }

    private static void fail(String message) {
        if (service != null) Toast.makeText(service, message, Toast.LENGTH_SHORT).show();
        finish();
    }

    public static void detach() { cancel(); }

    public static void cancel() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            MAIN.post(SettingsBackCoordinator::cancel);
            return;
        }
        SettingsReturnEngine.cancel();
        finish();
    }
}
