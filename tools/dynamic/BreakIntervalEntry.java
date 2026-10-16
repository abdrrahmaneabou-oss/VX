package com.vx.app.patch;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public final class BreakIntervalEntry {
    private BreakIntervalEntry() {}

    public static void attach(Activity activity) {
        try {
            final int[] hours = new int[] { BreakIntervalStore.getHours() };

            // Primary Colors for Dark Theme / Material UI
            int cardBgColor = 0xFF1E2638; // Dark Slate Blue
            int cardBorderColor = 0xFF334155;
            int accentBlue = 0xFF3B82F6;
            int accentGreen = 0xFF10B981;
            int accentPurple = 0xFF8B5CF6;
            int accentOrange = 0xFFF59E0B;
            int textColor = 0xFFFFFFFF;
            int secondaryColor = 0xFF94A3B8;

            // Scrollable Root Container
            ScrollView scrollView = new ScrollView(activity);
            scrollView.setBackgroundColor(0xFF0F172A); // Solid Dark Slate Background
            scrollView.setOutlineProvider(null);
            scrollView.setVerticalScrollBarEnabled(false);

            LinearLayout container = new LinearLayout(activity);
            container.setOrientation(LinearLayout.VERTICAL);
            int p = dp(activity, 16);
            container.setPadding(p, p, p, dp(activity, 40));

            // Dashboard Header
            TextView headerTitle = new TextView(activity);
            headerTitle.setText("مركز التحكم باللوحة الرئيسية");
            headerTitle.setTextSize(20f);
            headerTitle.setTypeface(null, Typeface.BOLD);
            headerTitle.setTextColor(textColor);
            headerTitle.setGravity(Gravity.START);
            container.addView(headerTitle, new LinearLayout.LayoutParams(-1, -2));

            TextView headerSub = new TextView(activity);
            headerSub.setText("تخصيص قواعد الحظر، الجمل، المواقع، والفترات الصارمة");
            headerSub.setTextSize(13f);
            headerSub.setTextColor(secondaryColor);
            LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(-1, -2);
            subParams.setMargins(0, dp(activity, 4), 0, dp(activity, 16));
            container.addView(headerSub, subParams);

            // Card 1: اختيار التطبيقات المحظورة
            container.addView(createCard(activity,
                    "اختيار التطبيقات",
                    "تحديد التطبيقات المراد حظرها لمنع التشتت",
                    "فتح قائمة التطبيقات",
                    accentBlue,
                    v -> openScreen(activity, "com.vx.app.selection.AppSelectionActivity")
            ));

            // Card 2: اختيار المواقع المحظورة
            container.addView(createCard(activity,
                    "اختيار المواقع",
                    "تحديد النطاقات والمواقع الإلكترونية المحظورة",
                    "فتح قائمة المواقع",
                    accentGreen,
                    v -> openScreen(activity, "com.vx.app.selection.WebsiteSelectionActivity")
            ));

            // Card 3: اختيار الجمل والكلمات والإعدادات المحظورة
            container.addView(createCard(activity,
                    "الجمل والإعدادات المحظورة",
                    "إدارة الكلمات والجمل وعناوين الإعدادات المحظورة ديناميكياً",
                    "تعديل قائمة الجمل",
                    accentPurple,
                    v -> activity.startActivity(new Intent(activity, DynamicLabelsActivity.class))
            ));

            // Card 4: اختيار مدة الحظر الصارم (مباشرة من الرئيسية)
            container.addView(createCard(activity,
                    "مدة الحظر الصارم",
                    "تحديد الوقت والمؤقت لبدء جلسة الحظر الصارم مباشرة",
                    "بدء/تحديد مؤقت الحظر",
                    accentOrange,
                    v -> openScreen(activity, "com.vx.app.patch.DurationActivity")
            ));

            // Card 5: اختيار الفترة بين استراحات 1 دقيقة
            LinearLayout breakCard = new LinearLayout(activity);
            breakCard.setOrientation(LinearLayout.VERTICAL);
            breakCard.setPadding(dp(activity, 16), dp(activity, 16), dp(activity, 16), dp(activity, 16));

            GradientDrawable breakBg = new GradientDrawable();
            breakBg.setColor(cardBgColor);
            breakBg.setCornerRadius(dp(activity, 16));
            breakBg.setStroke(dp(activity, 1), cardBorderColor);
            breakCard.setBackground(breakBg);

            TextView breakTitle = new TextView(activity);
            breakTitle.setText("الفترة بين استراحات (1 دقيقة)");
            breakTitle.setTextSize(16f);
            breakTitle.setTypeface(null, Typeface.BOLD);
            breakTitle.setTextColor(textColor);
            breakCard.addView(breakTitle, new LinearLayout.LayoutParams(-1, -2));

            TextView breakDesc = new TextView(activity);
            breakDesc.setText("تحديد تكرار الاستراحة التلقائية. مدة فتح القفل ثابتة: دقيقة واحدة.");
            breakDesc.setTextSize(12f);
            breakDesc.setTextColor(secondaryColor);
            LinearLayout.LayoutParams bDescParams = new LinearLayout.LayoutParams(-1, -2);
            bDescParams.setMargins(0, dp(activity, 4), 0, dp(activity, 12));
            breakCard.addView(breakDesc, bDescParams);

            // Counter Row
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);

            Button minus = new Button(activity);
            minus.setText("−");
            minus.setTextSize(22f);
            minus.setTextColor(textColor);
            minus.setBackground(createButtonBg(0xFF374151, dp(activity, 10)));
            row.addView(minus, new LinearLayout.LayoutParams(dp(activity, 52), dp(activity, 48)));

            TextView valueText = new TextView(activity);
            valueText.setTextSize(16f);
            valueText.setTypeface(null, Typeface.BOLD);
            valueText.setTextColor(accentBlue);
            valueText.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams valParams = new LinearLayout.LayoutParams(0, -2, 1f);
            valParams.setMargins(dp(activity, 8), 0, dp(activity, 8), 0);
            row.addView(valueText, valParams);

            Button plus = new Button(activity);
            plus.setText("+");
            plus.setTextSize(22f);
            plus.setTextColor(textColor);
            plus.setBackground(createButtonBg(0xFF374151, dp(activity, 10)));
            row.addView(plus, new LinearLayout.LayoutParams(dp(activity, 52), dp(activity, 48)));

            breakCard.addView(row, new LinearLayout.LayoutParams(-1, -2));

            Runnable refreshInterval = () -> {
                hours[0] = Math.max(1, Math.min(24, hours[0]));
                valueText.setText(formatInterval(hours[0]));
                minus.setEnabled(hours[0] > 1);
                plus.setEnabled(hours[0] < 24);
                minus.setAlpha(hours[0] > 1 ? 1.0f : 0.4f);
                plus.setAlpha(hours[0] < 24 ? 1.0f : 0.4f);
            };

            minus.setOnClickListener(v -> {
                if (hours[0] <= 1) return;
                hours[0]--;
                BreakIntervalStore.setHours(activity, hours[0]);
                refreshInterval.run();
                Toast.makeText(activity, "تم الحفظ: " + formatInterval(hours[0]), Toast.LENGTH_SHORT).show();
            });

            plus.setOnClickListener(v -> {
                if (hours[0] >= 24) return;
                hours[0]++;
                BreakIntervalStore.setHours(activity, hours[0]);
                refreshInterval.run();
                Toast.makeText(activity, "تم الحفظ: " + formatInterval(hours[0]), Toast.LENGTH_SHORT).show();
            });

            refreshInterval.run();

            LinearLayout.LayoutParams breakCardParams = new LinearLayout.LayoutParams(-1, -2);
            breakCardParams.setMargins(0, 0, 0, dp(activity, 16));
            container.addView(breakCard, breakCardParams);

            scrollView.addView(container, new ViewGroup.LayoutParams(-1, -2));

            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(-1, -1);
            activity.addContentView(scrollView, layoutParams);

        } catch (Throwable ignored) {
            // Overlay container safeguard
        }
    }

    private static View createCard(Activity activity, String title, String desc, String buttonText, int accentColor, View.OnClickListener listener) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(activity, 16), dp(activity, 16), dp(activity, 16), dp(activity, 16));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF1E2638);
        bg.setCornerRadius(dp(activity, 16));
        bg.setStroke(dp(activity, 1), 0xFF334155);
        card.setBackground(bg);

        TextView tvTitle = new TextView(activity);
        tvTitle.setText(title);
        tvTitle.setTextSize(16f);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(0xFFFFFFFF);
        card.addView(tvTitle, new LinearLayout.LayoutParams(-1, -2));

        TextView tvDesc = new TextView(activity);
        tvDesc.setText(desc);
        tvDesc.setTextSize(12f);
        tvDesc.setTextColor(0xFF94A3B8);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(-1, -2);
        descParams.setMargins(0, dp(activity, 4), 0, dp(activity, 12));
        card.addView(tvDesc, descParams);

        Button btn = new Button(activity);
        btn.setText(buttonText);
        btn.setAllCaps(false);
        btn.setTextSize(14f);
        btn.setTextColor(0xFFFFFFFF);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setBackground(createButtonBg(accentColor, dp(activity, 10)));
        btn.setOnClickListener(listener);

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(-1, dp(activity, 44));
        card.addView(btn, btnParams);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.setMargins(0, 0, 0, dp(activity, 16));
        card.setLayoutParams(cardParams);

        return card;
    }

    private static void openScreen(Activity activity, String className) {
        try {
            Intent intent = new Intent(activity, Class.forName(className));
            activity.startActivity(intent);
        } catch (Throwable t) {
            try {
                Intent intent = new Intent();
                intent.setClassName(activity.getPackageName(), className);
                activity.startActivity(intent);
            } catch (Throwable ignored) {
                Toast.makeText(activity, "تعذر فتح الشاشة المطلوبة", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private static GradientDrawable createButtonBg(int color, float radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
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
}
