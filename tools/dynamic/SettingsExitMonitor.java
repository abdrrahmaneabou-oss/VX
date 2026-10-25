package com.v1f.app.patch;

import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import java.util.List;

/** Detects a Settings departure independently of blocked labels. */
public final class SettingsExitMonitor {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final SettingsExitState STATE = new SettingsExitState();
    private static AccessibilityService service;
    private static Runnable poll;
    private static int generation;

    private SettingsExitMonitor() {}

    public static void initialize(AccessibilityService current) {
        if (current == null || !SettingsSessionGate.isActive()) { detach(); return; }
        if (Looper.myLooper() != Looper.getMainLooper()) {
            MAIN.post(() -> initialize(current));
            return;
        }
        if (service == current) return;
        detach();
        service = current;
        refresh(null);
        schedulePoll();
    }

    public static void observe(AccessibilityService current, AccessibilityEvent event) {
        if (!SettingsSessionGate.isActive()) { detach(); return; }
        initialize(current);
        if (event == null) return;
        int type = event.getEventType();
        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                || type == AccessibilityEvent.TYPE_WINDOWS_CHANGED
                || type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) refresh(event);
    }

    private static String foregroundPackage() {
        if (service == null) return null;
        try {
            PowerManager power = (PowerManager) service.getSystemService(Context.POWER_SERVICE);
            KeyguardManager keyguard = (KeyguardManager) service.getSystemService(Context.KEYGUARD_SERVICE);
            if ((power != null && !power.isInteractive())
                    || (keyguard != null && keyguard.isKeyguardLocked())) return "#screen-inactive";
            List<AccessibilityWindowInfo> windows = service.getWindows();
            if (windows != null) {
                try {
                    for (AccessibilityWindowInfo window : windows) {
                        if (window.getType() != AccessibilityWindowInfo.TYPE_APPLICATION
                                || !window.isFocused()) continue;
                        AccessibilityNodeInfo root = window.getRoot();
                        if (root == null) continue;
                        try {
                            if (root.getPackageName() != null) return root.getPackageName().toString();
                        } finally { root.recycle(); }
                    }
                } finally {
                    for (AccessibilityWindowInfo window : windows) window.recycle();
                }
            }
            AccessibilityNodeInfo root = service.getRootInActiveWindow();
            if (root != null) {
                try {
                    if (root.getPackageName() != null) return root.getPackageName().toString();
                } finally { root.recycle(); }
            }
        } catch (RuntimeException ignored) {}
        return null;
    }

    private static void refresh(AccessibilityEvent event) {
        if (!SettingsSessionGate.isActive()) {
            SettingsBackCoordinator.cancel();
            return;
        }
        boolean ownNavigation = SettingsBackCoordinator.isNavigating();
        // Read the focused application, not toast/dialog event package names.
        // Unknown snapshots preserve state until the next observation.
        String pkg = ownNavigation ? null : foregroundPackage();
        if (STATE.observe(ownNavigation, pkg)) {
            SettingsBackCoordinator.triggerSettingsExit(service);
        }
    }

    private static void schedulePoll() {
        if (poll != null) MAIN.removeCallbacks(poll);
        if (service == null) return;
        final int token = generation;
        poll = () -> {
            if (token != generation || service == null) return;
            poll = null;
            refresh(null);
            schedulePoll();
        };
        MAIN.postDelayed(poll, 250L);
    }

    public static void detach() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            MAIN.post(SettingsExitMonitor::detach);
            return;
        }
        generation++;
        if (poll != null) MAIN.removeCallbacks(poll);
        poll = null;
        STATE.clear();
        service = null;
    }
}
