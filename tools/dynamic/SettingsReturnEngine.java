package com.v1f.app.patch;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import java.util.List;

/**
 * One-shot return stage. Always invoked AFTER the two unconditional initial
 * BACK requests. Events with a new window are read before stale root snapshots.
 * A bounded-frequency fallback examines focused application windows when OEM
 * window events are delayed or lost; no Shizuku or background force-stop.
 */
public final class SettingsReturnEngine {
    private static final String SETTINGS = "com.android.settings";
    private static final String SELF = "com.v1f.app";
    private static final String PREF_FILE = "v2f_settings_return";
    private static final String PREF_STATE = "pending_stage";
    private static final long FALLBACK_CONFIRM_MS = 160L;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final SettingsReturnState MACHINE = new SettingsReturnState();

    private static AccessibilityService service;
    private static Runnable pollTask;
    private static int generation;
    private static long armedAt;
    private static long lastExitAt;
    private static String lastPolledPackage;
    private static int matchingPolls;

    private SettingsReturnEngine() {}

    /**
     * Called from onServiceConnected, so TYPE_WINDOWS_CHANGED and interactive
     * window snapshots are available even if the original XML omitted them.
     */
    public static void initialize(final AccessibilityService current) {
        if (current == null) return;
        if (Looper.myLooper() != Looper.getMainLooper()) {
            MAIN.post(() -> initialize(current));
            return;
        }
        service = current;
        try {
            AccessibilityServiceInfo info = current.getServiceInfo();
            if (info != null) {
                int wantedEvents = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                        | AccessibilityEvent.TYPE_WINDOWS_CHANGED
                        | AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
                int wantedFlags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
                if ((info.eventTypes & wantedEvents) != wantedEvents
                        || (info.flags & wantedFlags) != wantedFlags) {
                    info.eventTypes |= wantedEvents;
                    info.flags |= wantedFlags;
                    current.setServiceInfo(info);
                }
            }
        } catch (RuntimeException ignored) {
            // Original configuration continues to work if OEM rejects a flag.
        }
        if (MACHINE.getState() == SettingsReturnState.IDLE) {
            int persisted = current.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
                    .getInt(PREF_STATE, SettingsReturnState.IDLE);
            MACHINE.restore(persisted);
            if (MACHINE.getState() != SettingsReturnState.IDLE) {
                armedAt = SystemClock.elapsedRealtime();
                lastExitAt = armedAt - FALLBACK_CONFIRM_MS;
                schedulePoll();
            }
        }
    }

    public static void arm(final AccessibilityService current) {
        if (current == null) return;
        if (Looper.myLooper() != Looper.getMainLooper()) {
            MAIN.post(() -> arm(current));
            return;
        }
        service = current;
        generation++;
        removePoll();
        MACHINE.arm();
        persist();
        armedAt = SystemClock.elapsedRealtime();
        lastExitAt = 0L;
        lastPolledPackage = null;
        matchingPolls = 0;

        // Immediate snapshot also detects Home if the user left before arm().
        examineFocusedWindow();
        schedulePoll();
    }

    public static void observe(final AccessibilityService current,
                               final AccessibilityEvent event) {
        if (current == null || event == null || MACHINE.getState() == SettingsReturnState.IDLE)
            return;
        if (Looper.myLooper() != Looper.getMainLooper()) {
            MAIN.post(() -> observe(current, event));
            return;
        }
        service = current;
        final int kind = event.getEventType();

        // A WINDOW_STATE_CHANGED event describes the NEW window. It must take
        // precedence over getRootInActiveWindow(), which can still be stale.
        // Never inspect the old root again in the same callback.
        if (kind == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            String pkg = eventPackage(event);
            if (pkg != null && !pkg.startsWith(SELF)) {
                transition(pkg);
                return;
            }
        }

        // TYPE_WINDOWS_CHANGED can describe the window LOSING focus.
        // Its event package is not necessarily the new foreground package.
        // Inspect focused windows instead of trusting that package.

        // Content-change events are never foreground evidence by themselves.
        // They can wake a fresh focused-window inspection, but cannot use their
        // own package field to claim Settings is foreground.
        if (kind == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                || kind == AccessibilityEvent.TYPE_WINDOWS_CHANGED
                || kind == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            examineFocusedWindow();
        }
    }

    private static String eventPackage(AccessibilityEvent event) {
        CharSequence pkg = event.getPackageName();
        return pkg == null ? null : pkg.toString();
    }

    private static void transition(String packageName) {
        int before = MACHINE.getState();
        boolean returned = MACHINE.accept(packageName);
        int after = MACHINE.getState();
        if (before != after) {
            if (after == SettingsReturnState.WAITING_FOR_RETURN) {
                lastExitAt = SystemClock.elapsedRealtime();
                lastPolledPackage = null;
                matchingPolls = 0;
            }
            persist();
        }
        if (returned) {
            generation++;
            removePoll();
            AccessibilityService current = service;
            if (current != null && !StrictBlockPolicy.allowsAll()) {
                // The user specified an immediate pair with no verification
                // and no intentional delay between these two requests.
                safeBack(current);
                safeBack(current);
            }
        }
    }

    private static void examineFocusedWindow() {
        if (MACHINE.getState() == SettingsReturnState.IDLE || service == null) return;
        String pkg = null;
        boolean strongWindow = false;
        try {
            List<AccessibilityWindowInfo> windows = service.getWindows();
            if (windows != null) {
                for (AccessibilityWindowInfo w : windows) {
                    if (w == null || w.getType() != AccessibilityWindowInfo.TYPE_APPLICATION
                            || !w.isFocused()) continue;
                    AccessibilityNodeInfo root = w.getRoot();
                    if (root == null || root.getPackageName() == null) continue;
                    String found = root.getPackageName().toString();
                    // The block shield isn't evidence of Settings leaving.
                    if (found.startsWith(SELF)) return;
                    pkg = found;
                    strongWindow = true;
                    break;
                }
            }
        } catch (RuntimeException ignored) {}

        if (pkg == null) {
            try {
                AccessibilityNodeInfo root = service.getRootInActiveWindow();
                if (root != null && root.getPackageName() != null) {
                    String found = root.getPackageName().toString();
                    if (!found.startsWith(SELF)) pkg = found;
                }
            } catch (RuntimeException ignored) {}
        }
        if (pkg == null) return;

        // If only the root is available, demand two stable observations.
        // A single stale root immediately after HOME must not consume BACKs.
        if (!strongWindow) {
            if (!pkg.equals(lastPolledPackage)) {
                lastPolledPackage = pkg;
                matchingPolls = 1;
                return;
            }
            if (++matchingPolls < 2) return;
            if (MACHINE.getState() == SettingsReturnState.WAITING_FOR_RETURN
                    && lastExitAt > 0
                    && SystemClock.elapsedRealtime() - lastExitAt < FALLBACK_CONFIRM_MS)
                return;
        }
        transition(pkg);
    }

    private static void safeBack(AccessibilityService current) {
        try {
            current.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
        } catch (RuntimeException ignored) {}
    }

    private static void persist() {
        if (service == null) return;
        SharedPreferences.Editor editor = service
                .getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE).edit();
        if (MACHINE.getState() == SettingsReturnState.IDLE) editor.remove(PREF_STATE);
        else editor.putInt(PREF_STATE, MACHINE.getState());
        editor.apply();
    }

    private static void schedulePoll() {
        if (MACHINE.getState() == SettingsReturnState.IDLE || service == null) return;
        removePoll();
        final int token = generation;
        long age = SystemClock.elapsedRealtime() - armedAt;
        final long delay = age < 2500L ? 75L : age < 30000L ? 180L : 550L;
        pollTask = () -> {
            if (token != generation || MACHINE.getState() == SettingsReturnState.IDLE) return;
            pollTask = null;
            examineFocusedWindow();
            schedulePoll();
        };
        MAIN.postDelayed(pollTask, delay);
    }

    private static void removePoll() {
        if (pollTask != null) MAIN.removeCallbacks(pollTask);
        pollTask = null;
    }

    public static void cancel() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            MAIN.post(SettingsReturnEngine::cancel);
            return;
        }
        generation++;
        removePoll();
        MACHINE.clear();
        persist();
        service = null;
        lastExitAt = 0;
        lastPolledPackage = null;
        matchingPolls = 0;
    }
}
