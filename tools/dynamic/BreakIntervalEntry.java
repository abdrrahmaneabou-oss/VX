package com.v1f.app.patch;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public final class BreakIntervalEntry {
    private BreakIntervalEntry() {}

    public static void attach(Activity activity) {
        try {
            final int[] hours = new int[] { BreakIntervalStore.getHours() };

            LinearLayout card = new LinearLayout(activity);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(activity, 14), dp(activity, 12), dp(activity, 14), dp(activity, 12));
            card.setElevation(dp(activity, 8));

            GradientDrawable background = new GradientDrawable();
            background.setColor(resolveColor(activity, android.R.attr.colorBackground, 0xff202124));
            background.setCornerRadius(dp(activity, 16));
            background.setStroke(dp(activity, 1), resolveColor(activity, android.R.attr.textColorSecondary, 0xff777777));
            card.setBackground(background);

            int textColor = resolveColor(activity, android.R.attr.textColorPrimary, 0xffffffff);
            int secondaryColor = resolveColor(activity, android.R.attr.textColorSecondary, 0xffb0b0b0);

            TextView title = new TextView(activity);
            title.setText("فترة فتح القفل التلقائي");
            title.setTextSize(17f);
            title.setTextColor(textColor);
            title.setGravity(Gravity.CENTER);
            card.addView(title, new LinearLayout.LayoutParams(-1, -2));

            TextView hint = new TextView(activity);
            hint.setText("مدة الفتح ثابتة: دقيقة واحدة");
            hint.setTextSize(12f);
            hint.setTextColor(secondaryColor);
            hint.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(-1, -2);
            hintParams.setMargins(0, dp(activity, 2), 0, dp(activity, 6));
            card.addView(hint, hintParams);

            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);

            Button minus = new Button(activity);
            minus.setText("−");
            minus.setTextSize(20f);
            minus.setAllCaps(false);
            row.addView(minus, new LinearLayout.LayoutParams(dp(activity, 58), -2));

            TextView value = new TextView(activity);
            value.setTextSize(16f);
            value.setTextColor(textColor);
            value.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(0, -2, 1f);
            valueParams.setMargins(dp(activity, 8), 0, dp(activity, 8), 0);
            row.addView(value, valueParams);

            Button plus = new Button(activity);
            plus.setText("+");
            plus.setTextSize(20f);
            plus.setAllCaps(false);
            row.addView(plus, new LinearLayout.LayoutParams(dp(activity, 58), -2));

            card.addView(row, new LinearLayout.LayoutParams(dp(activity, 300), -2));

            Runnable refresh = () -> {
                hours[0] = Math.max(1, Math.min(24, hours[0]));
                value.setText(formatInterval(hours[0]));
                minus.setEnabled(hours[0] > 1);
                plus.setEnabled(hours[0] < 24);
            };

            minus.setOnClickListener(v -> {
                if (hours[0] <= 1) return;
                hours[0]--;
                BreakIntervalStore.setHours(activity, hours[0]);
                refresh.run();
                Toast.makeText(activity, "تم الحفظ: " + formatInterval(hours[0]), Toast.LENGTH_SHORT).show();
            });

            plus.setOnClickListener(v -> {
                if (hours[0] >= 24) return;
                hours[0]++;
                BreakIntervalStore.setHours(activity, hours[0]);
                refresh.run();
                Toast.makeText(activity, "تم الحفظ: " + formatInterval(hours[0]), Toast.LENGTH_SHORT).show();
            });

            refresh.run();

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            int margin = dp(activity, 16);
            params.setMargins(margin, margin, margin, dp(activity, 88));
            activity.addContentView(card, params);
        } catch (Throwable ignored) {
            // Optional UI must never prevent MainActivity from opening.
        }
    }

    private static String formatInterval(int hours) {
        if (hours == 1) return "كل ساعة واحدة";
        if (hours == 2) return "كل ساعتين";
        if (hours >= 3 && hours <= 10) return "كل " + hours + " ساعات";
        return "كل " + hours + " ساعة";
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    private static int resolveColor(Activity activity, int attr, int fallback) {
        TypedValue value = new TypedValue();
        if (!activity.getTheme().resolveAttribute(attr, value, true)) return fallback;
        if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT && value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            return value.data;
        }
        try {
            return activity.getColor(value.resourceId);
        } catch (Throwable ignored) {
            return fallback;
        }
    }
}
