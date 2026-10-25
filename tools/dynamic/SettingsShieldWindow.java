package com.v1f.app.patch;

import android.content.Context;
import android.graphics.Point;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

/** Settings shield: triple display size, 75% transparent, consumes touches. */
public final class SettingsShieldWindow {
    private SettingsShieldWindow() {}

    public static void configure(View view, WindowManager.LayoutParams params) {
        WindowManager wm = (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
        Point size = new Point();
        wm.getDefaultDisplay().getRealSize(size);
        params.width = size.x * 3;
        params.height = size.y * 3;
        params.gravity = Gravity.CENTER;
        params.x = 0;
        params.y = 0;
        params.alpha = 0.25f;
        params.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        params.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        view.setClickable(true);
        view.setOnTouchListener((v, event) -> true);
    }
}
