package com.v1f.app.patch;

import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import java.util.List;

/** Sky-blue touch-consuming bottom 5% strip while Settings is foreground. */
public final class SettingsBottomStrip {
    private static final String SETTINGS = "com.android.settings";
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static AccessibilityService service;
    private static WindowManager manager;
    private static View strip;
    private static WindowManager.LayoutParams params;

    private SettingsBottomStrip() {}

    /**
     * Called by the existing BreakPauseController service tick and event path,
     * with NO new timer.  Never enable the strip outside an active session.
     */
    public static void update(AccessibilityService current) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            MAIN.post(() -> update(current));
            return;
        }
        if (current == null || !SettingsSessionGate.isActive()) {
            hide();
            return;
        }
        if (service != current) {
            detach();
            service = current;
            manager = (WindowManager) current.getSystemService(Context.WINDOW_SERVICE);
        }
        refresh();
    }

    private static boolean settingsForeground() {
        if (service == null) return false;
        try {
            PowerManager power = (PowerManager) service.getSystemService(Context.POWER_SERVICE);
            KeyguardManager keyguard = (KeyguardManager) service.getSystemService(Context.KEYGUARD_SERVICE);
            if ((power != null && !power.isInteractive())
                    || (keyguard != null && keyguard.isKeyguardLocked())) return false;
            List<AccessibilityWindowInfo> windows = service.getWindows();
            if (windows != null) {
                try {
                    for (AccessibilityWindowInfo window : windows) {
                        // Ignore our accessibility overlays: they are not applications.
                        if (window.getType() != AccessibilityWindowInfo.TYPE_APPLICATION
                                || !window.isFocused()) continue;
                        AccessibilityNodeInfo root = window.getRoot();
                        if (root == null) continue;
                        try { return SETTINGS.contentEquals(root.getPackageName() == null
                                ? "" : root.getPackageName()); }
                        finally { root.recycle(); }
                    }
                } finally {
                    for (AccessibilityWindowInfo window : windows) window.recycle();
                }
            }
            AccessibilityNodeInfo root = service.getRootInActiveWindow();
            if (root != null) {
                try { return SETTINGS.contentEquals(root.getPackageName() == null
                        ? "" : root.getPackageName()); }
                finally { root.recycle(); }
            }
        } catch (RuntimeException ignored) {}
        // Remove the strip if the foreground cannot be established.
        return false;
    }

    private static void refresh() {
        if (manager == null || !SettingsSessionGate.isActive()) {
            hide();
            return;
        }
        if (!settingsForeground()) {
            hide();
            return;
        }
        try {
            Point size = new Point();
            manager.getDefaultDisplay().getRealSize(size);
            int height = Math.max(1, Math.round(size.y * 0.05f));
            if (strip != null) {
                if (params.width != size.x || params.height != height) {
                    params.width = size.x;
                    params.height = height;
                    manager.updateViewLayout(strip, params);
                }
                return;
            }
            View view = new View(service);
            view.setBackgroundColor(Color.rgb(135, 206, 235));
            view.setClickable(true);
            view.setFocusable(false);
            view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            view.setOnTouchListener((v, event) -> true);
            WindowManager.LayoutParams layout = new WindowManager.LayoutParams(
                    size.x, height,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT);
            layout.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            layout.x = 0;
            layout.y = 0;
            layout.alpha = 1f;
            layout.setTitle("Settings bottom touch shield");
            if (Build.VERSION.SDK_INT >= 28)
                layout.layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            if (Build.VERSION.SDK_INT >= 30) layout.setFitInsetsTypes(0);
            manager.addView(view, layout);
            strip = view;
            params = layout;
        } catch (RuntimeException ignored) {
            hide();
        }
    }

    private static void hide() {
        if (strip != null && manager != null) {
            try { manager.removeViewImmediate(strip); } catch (RuntimeException ignored) {}
        }
        strip = null;
        params = null;
    }

    public static void detach() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            MAIN.post(SettingsBottomStrip::detach);
            return;
        }
        hide();
        service = null;
        manager = null;
    }
}
