package com.v1f.app.patch;

import android.accessibilityservice.AccessibilityService;
import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.lang.ref.WeakReference;

/**
 * One central pause barrier for the original protection engine and all V2F
 * additions. During the scheduled break no lock or Settings recovery may run. Polling also removes an ALREADY-visible lock/shield.
 */
public final class BreakPauseController {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final long CHECK_MS = 200L;
    private static AccessibilityService service;
    private static Runnable servicePoll;
    private static Runnable lockPoll;
    private static WeakReference<Activity> lockActivity;
    private static boolean wasBreak;
    private static int generation;

    private BreakPauseController() {}

    public static boolean isBreakActive() {
        return BreakPauseRules.breakActive(SettingsSessionGate.isSessionRunning(), StrictBlockPolicy.allowsAll());
    }

    /** Check this before the stock ProtectionEngine dispatches any block. */
    public static boolean suppressBlockEvent(AccessibilityService current) {
        if (!isBreakActive()) return false;
        if (Looper.myLooper() == Looper.getMainLooper()) {
            onBreakStarted();
        } else {
            MAIN.post(BreakPauseController::onBreakStarted);
        }
        return true;
    }

    private static void onBreakStarted() {
        if (wasBreak) return;
        wasBreak = true;
        // Cancel in-flight callbacks too, not only newly-arriving block events.
        SettingsBackCoordinator.cancel();
        closeVisibleLock();
    }

    public static void attach(final AccessibilityService current) {
        if (current == null) return;
        if (Looper.myLooper() != Looper.getMainLooper()) {
            MAIN.post(() -> attach(current));
            return;
        }
        if (service == current && servicePoll != null) return;
        detach();
        service = current;
        generation++;
        final int token = generation;
        servicePoll = new Runnable() {
            @Override public void run() {
                if (service != current || token != generation || servicePoll != this) return;
                boolean inBreak = isBreakActive();
                if (inBreak) {
                    onBreakStarted();
                    closeVisibleLock();
                } else if (wasBreak) {
                    wasBreak = false;
                    if (SettingsSessionGate.isActive()) {
                        SettingsExitMonitor.initialize(current);
                        // The foreground app/site can still be open when a break
                        // ends. Re-evaluate without waiting for user interaction.
                        MAIN.postDelayed(() -> recheckForeground(current, token), 150L);
                    }
                }
                MAIN.postDelayed(this, CHECK_MS);
            }
        };
        MAIN.post(servicePoll);
    }

    private static void recheckForeground(AccessibilityService current, int token) {
        if (token != generation || service != current
                || !SettingsSessionGate.isActive()) return;
        AccessibilityNodeInfo node = null;
        AccessibilityEvent event = null;
        try {
            node = current.getRootInActiveWindow();
            if (node == null || node.getPackageName() == null) return;
            String pkg = node.getPackageName().toString();
            if (pkg.equals(current.getPackageName())) return;
            event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
            event.setPackageName(pkg);
            event.setClassName("android.app.Activity");
            current.onAccessibilityEvent(event);
        } catch (Throwable ignored) {
        } finally {
            if (node != null) try { node.recycle(); } catch (RuntimeException ignored) {}
            if (event != null) event.recycle();
        }
    }

    public static void watchLock(final Activity activity) {
        if (activity == null) return;
        if (Looper.myLooper() != Looper.getMainLooper()) {
            MAIN.post(() -> watchLock(activity));
            return;
        }
        lockActivity = new WeakReference<>(activity);
        closeVisibleLock();
        if (lockPoll != null) MAIN.removeCallbacks(lockPoll);
        lockPoll = new Runnable() {
            @Override public void run() {
                if (lockPoll != this) return;
                Activity current = lockActivity == null ? null : lockActivity.get();
                if (current == null || current.isDestroyed() || current.isFinishing()) {
                    lockPoll = null;
                    lockActivity = null;
                    return;
                }
                closeVisibleLock();
                if (lockPoll == this) MAIN.postDelayed(this, CHECK_MS);
            }
        };
        MAIN.postDelayed(lockPoll, CHECK_MS);
    }

    private static void closeVisibleLock() {
        Activity activity = lockActivity == null ? null : lockActivity.get();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        if (BreakPauseRules.closeLock(SettingsSessionGate.isSessionRunning(), StrictBlockPolicy.allowsAll())) {
            // Direct finish bypasses the normal 3-second manual EXIT gate.
            activity.finish();
        }
    }

    public static void detach() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            MAIN.post(BreakPauseController::detach);
            return;
        }
        generation++;
        if (servicePoll != null) MAIN.removeCallbacks(servicePoll);
        servicePoll = null;
        service = null;
        wasBreak = false;
        // LockActivity may outlive the service, so keep its separate watcher.
    }
}
