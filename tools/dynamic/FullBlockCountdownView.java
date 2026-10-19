package com.v1f.app.patch;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

/** A lifecycle-bound label above the existing lock screen, with no navigation changes. */
public final class FullBlockCountdownView extends TextView {
    private static final String TAG = "vx_hourly_countdown";
    private final Activity host;
    private boolean running;
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!running || host.isFinishing() || host.isDestroyed()) return;
            Intent intent = host.getIntent();
            String text = "";
            // Website locks must never inherit an application countdown.
            if (intent != null && intent.getStringExtra("blocked_domain") == null)
                text = FullBlockRules.countdownText(RandomFullBlock.remainingMillis(
                        host, intent.getStringExtra("blocked_package")));
            if (!text.contentEquals(getText())) setText(text);
            setVisibility(text.isEmpty() ? View.GONE : View.VISIBLE);
            postDelayed(this, 1000L);
        }
    };

    private FullBlockCountdownView(Activity activity) {
        super(activity);
        host = activity;
        setTag(TAG);
        setTextColor(Color.WHITE);
        setTextSize(19);
        setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        setGravity(Gravity.CENTER);
        setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        setPadding(dp(16), dp(14), dp(16), dp(14));
        setClickable(false);
        setFocusable(false);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.argb(235, 18, 22, 35));
        bg.setCornerRadius(dp(16));
        setBackground(bg);
        setVisibility(View.GONE);
    }

    private int dp(int value) {
        return (int)(value * getResources().getDisplayMetrics().density + 0.5f);
    }

    public static void attach(Activity activity) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content == null) return;
        FullBlockCountdownView view = content.findViewWithTag(TAG);
        if (view == null) {
            view = new FullBlockCountdownView(activity);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP | Gravity.CENTER_HORIZONTAL);
            params.setMargins(view.dp(24), view.dp(56), view.dp(24), 0);
            content.addView(view, params);
        }
        view.bringToFront();
        view.running = true;
        view.removeCallbacks(view.tick);
        view.tick.run();
    }

    public static void stop(Activity activity) {
        View content = activity.findViewById(android.R.id.content);
        if (content == null) return;
        FullBlockCountdownView view = content.findViewWithTag(TAG);
        if (view != null) view.stopTicks();
    }

    private void stopTicks() {
        running = false;
        removeCallbacks(tick);
    }

    @Override protected void onDetachedFromWindow() {
        stopTicks();
        super.onDetachedFromWindow();
    }
}
