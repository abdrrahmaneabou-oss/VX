package com.v1f.app.patch;

import android.accessibilityservice.AccessibilityService;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import java.util.List;

/**
 * Independent second phase of the Settings escape:
 * once initial BACK x2 is sent, observe Settings leaving the foreground;
 * on its first subsequent return, send BACK x2 immediately, once.
 *
 * Window events drive the fast path; a short-lived check loop is a fallback
 * when an OEM fails to report a foreground transition. No Shizuku needed.
 */
public final class SettingsReturnEngine {
    private static final String SETTINGS_PACKAGE = "com.android.settings";
    private static final long CHECK_INTERVAL_MS = 90L;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    // 0 = inactive, 1 = waiting for Settings to leave, 2 = waiting for return.
    private static int state;
    private static AccessibilityService service;
    private static Runnable checkTask;
    private static int generation;

    private SettingsReturnEngine() {}

    public static void arm(final AccessibilityService current) {
        if (current == null) return;
        if (Looper.myLooper() != Looper.getMainLooper()) {
            MAIN.post(() -> arm(current));
            return;
        }
        generation++;
        cancelScheduled();
        service = current;
        state = 1;
        // Start directly after the two initial BACK calls.
        checkForeground(null);
        scheduleCheck();
    }

    public static void observe(final AccessibilityService current,
                               final AccessibilityEvent event) {
        if (current == null || event == null || state == 0) return;
        if (Looper.myLooper() != Looper.getMainLooper()) {
            MAIN.post(() -> observe(current, event));
            return;
        }
        int type = event.getEventType();
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && type != AccessibilityEvent.TYPE_WINDOWS_CHANGED
                && type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            return;
        }
        service = current;
        checkForeground(event);
    }

    private static void checkForeground(AccessibilityEvent event) {
        if (state == 0 || service == null) return;
        String foreground = foregroundPackage(service, event);
        if (foreground == null) return; // Unknown != Settings is background.
        if (state == 1) {
            if (!SETTINGS_PACKAGE.equals(foreground)) {
                state = 2;
            }
        } else if (state == 2 && SETTINGS_PACKAGE.equals(foreground)) {
            // Consume before dispatch: generated events must not repeat the pair.
            state = 0;
            generation++;
            cancelScheduled();
            AccessibilityService target = service;
            service = null;
            target.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
            target.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
        }
    }

    private static String foregroundPackage(AccessibilityService current,
                                            AccessibilityEvent event) {
        // Prefer the focused APPLICATION window, not the transient shield.
        try {
            List<AccessibilityWindowInfo> windows = current.getWindows();
            if (windows != null) {
                for (AccessibilityWindowInfo window : windows) {
                    if (window == null || !window.isFocused()
                            || window.getType() != AccessibilityWindowInfo.TYPE_APPLICATION)
                        continue;
                    AccessibilityNodeInfo root = window.getRoot();
                    if (root != null && root.getPackageName() != null)
                        return root.getPackageName().toString();
                }
            }
        } catch (RuntimeException ignored) {}

        try {
            AccessibilityNodeInfo root = current.getRootInActiveWindow();
            if (root != null && root.getPackageName() != null) {
                String name = root.getPackageName().toString();
                // V2F's own overlay does not prove Settings left foreground.
                if (!name.startsWith("com.v1f.app")) return name;
            }
        } catch (RuntimeException ignored) {}

        // Only window transitions provide a useful fallback package.
        if (event != null &&
                (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    || event.getEventType() == AccessibilityEvent.TYPE_WINDOWS_CHANGED)
                && event.getPackageName() != null) {
            String name = event.getPackageName().toString();
            if (!name.startsWith("com.v1f.app")) return name;
        }
        return null;
    }

    private static void scheduleCheck() {
        if (state == 0) return;
        if (checkTask != null) MAIN.removeCallbacks(checkTask);
        final int token = generation;
        checkTask = () -> {
            if (token != generation || state == 0) return;
            checkTask = null;
            checkForeground(null);
            scheduleCheck();
        };
        MAIN.postDelayed(checkTask, CHECK_INTERVAL_MS);
    }

    private static void cancelScheduled() {
        if (checkTask != null) MAIN.removeCallbacks(checkTask);
        checkTask = null;
    }

    public static void cancel() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            MAIN.post(SettingsReturnEngine::cancel);
            return;
        }
        generation++;
        state = 0;
        service = null;
        cancelScheduled();
    }
}
