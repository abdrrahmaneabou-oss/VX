package com.v1f.app.patch;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;
import com.v1f.app.protection.overlay.MeteorShieldOverlay;

/** Shield -> three BACKs -> launch Settings -> wait 500ms -> three BACKs -> hide. */
public final class SettingsBackCoordinator {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final String SETTINGS = "com.android.settings";
    private static AccessibilityService service;
    private static MeteorShieldOverlay shield;
    private static Runnable pending;
    private static Runnable deadlineTask;
    private static long deadlineAt;
    private static final long MAX_DURATION_MS = 7500L;
    private static int generation;
    private static int stage; // 0 idle, 1 waiting 500ms after launch, 2 final BACKs, 3 initial BACKs
    private static long quietUntil;

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
        stage = 3;
        deadlineAt = SystemClock.elapsedRealtime() + MAX_DURATION_MS;
        try {
            if (shield == null || !shield.show()) {
                fail("Could not show protection shield");
                return;
            }
        } catch (RuntimeException error) {
            fail("Could not show protection shield");
            return;
        }
        // Independent watchdog: never reset by launch or BACK retries.
        final int token = generation;
        deadlineTask = () -> {
            if (token == generation && stage != 0) finish();
        };
        MAIN.postDelayed(deadlineTask, MAX_DURATION_MS);
        // Let the attached shield receive a frame before issuing initial BACKs.
        schedule(() -> sendBack(1), 32L);
    }

    private static void openSettings() {
        if (expired() || service == null) return;
        stage = 1;
        try {
            Intent intent = service.getPackageManager().getLaunchIntentForPackage(SETTINGS);
            if (intent == null) intent = new Intent(Settings.ACTION_SETTINGS).setPackage(SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            service.startActivity(intent);
            // Fixed delay; window events cannot shorten or extend it.
            schedule(SettingsBackCoordinator::beginBacks, 500L);
        } catch (RuntimeException error) {
            fail("Could not open Settings");
        }
    }

    private static boolean expired() {
        if (stage == 0) return true;
        if (SystemClock.elapsedRealtime() < deadlineAt) return false;
        finish();
        return true;
    }

    public static boolean observe(AccessibilityService current, AccessibilityEvent event) {
        if (stage == 0) return SystemClock.elapsedRealtime() < quietUntil;
        if (expired()) return true;
        // Consume navigation events without using them to start the final BACKs.
        return true;
    }

    private static void beginBacks() {
        if (stage != 1) return;
        stage = 2;
        removePending();
        sendBack(1);
    }

    private static void sendBack(final int number) {
        if (expired() || (stage != 2 && stage != 3) || service == null) return;
        boolean accepted = false;
        try { accepted = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK); }
        catch (RuntimeException ignored) {}
        if (!accepted) {
            schedule(() -> sendBack(number), 150L);
        } else if (number < 3) {
            schedule(() -> sendBack(number + 1), 300L);
        } else if (stage == 3) {
            // Let the third global BACK settle before bringing Settings forward.
            schedule(SettingsBackCoordinator::openSettings, 300L);
        } else {
            finish();
        }
    }

    private static void schedule(Runnable action, long delay) {
        removePending();
        final int token = generation;
        pending = () -> {
            if (token != generation || expired()) return;
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
        if (deadlineTask != null) MAIN.removeCallbacks(deadlineTask);
        deadlineTask = null;
        deadlineAt = 0L;
        stage = 0;
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
