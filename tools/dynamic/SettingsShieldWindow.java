package com.v1f.app.patch;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import java.util.WeakHashMap;

/** Completely opaque Settings shield; solid black with red lock and elapsed seconds. */
public final class SettingsShieldWindow {
    private static final WeakHashMap<View, Long> STARTS = new WeakHashMap<>();
    private static final int RED = Color.rgb(235, 36, 54);
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
        params.alpha = 1.0f; // Zero transparency.
        params.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        params.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        view.setBackgroundColor(Color.BLACK);
        view.setClickable(true);
        view.setOnTouchListener((v, event) -> true);
        STARTS.put(view, SystemClock.elapsedRealtime());
    }

    /** Rendered in place of the stock MeteorShieldView artwork. */
    public static void draw(View view, Canvas canvas) {
        canvas.drawColor(Color.BLACK);
        Long start = STARTS.get(view);
        if (start == null) {
            start = SystemClock.elapsedRealtime();
            STARTS.put(view, start);
        }
        String seconds = Long.toString(1L + Math.max(0L, SystemClock.elapsedRealtime() - start) / 1000L);
        float d = Math.max(1f, view.getResources().getDisplayMetrics().density);
        float cx = canvas.getWidth() / 2f;
        float cy = canvas.getHeight() / 2f;
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(RED);
        paint.setStrokeWidth(10f * d);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRoundRect(new RectF(cx - 35f * d, cy - 83f * d,
                cx + 35f * d, cy + 23f * d), 34f * d, 34f * d, paint);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(new RectF(cx - 58f * d, cy - 28f * d,
                cx + 58f * d, cy + 64f * d), 13f * d, 13f * d, paint);
        paint.setColor(Color.WHITE);
        paint.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD));
        paint.setTextSize(38f * d);
        paint.setTextAlign(Paint.Align.CENTER);
        Paint.FontMetrics fm = paint.getFontMetrics();
        canvas.drawText(seconds, cx,
                cy + 18f * d - (fm.ascent + fm.descent) / 2f, paint);
        if (view.isAttachedToWindow()) view.postInvalidateDelayed(100L);
    }
}
