package com.vx.app.patch;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.provider.Settings;
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

            // Cosmic Space & Meteor Glow Theme Colors
            int spaceBgColor = 0xFF0B0F19;     // Deep Void Space
            int cardBgColor = 0xFF131B2E;      // Cosmic Glass Navy
            int borderGlowColor = 0xFF311B92;  // Cosmic Purple Glow Border
            int redGlowAccent = 0xFFEF4444;    // Glowing Meteor Red
            int purpleAccent = 0xFF8B5CF6;     // Nebula Purple
            int cyanAccent = 0xFF06B6D4;       // Starlight Cyan
            int emeraldAccent = 0xFF10B981;    // Meteor Green
            int amberAccent = 0xFFF59E0B;      // Cosmic Amber
            int textColor = 0xFFFFFFFF;
            int secondaryColor = 0xFF94A3B8;

            // Scrollable Root Container
            ScrollView scrollView = new ScrollView(activity);
            scrollView.setBackgroundColor(spaceBgColor);
            scrollView.setOutlineProvider(null);
            scrollView.setVerticalScrollBarEnabled(false);

            LinearLayout container = new LinearLayout(activity);
            container.setOrientation(LinearLayout.VERTICAL);
            int p = dp(activity, 16);
            container.setPadding(p, p, p, dp(activity, 40));

            // Header Title with Glowing Cosmic Design
            TextView headerTitle = new TextView(activity);
            headerTitle.setText("لوحة التحكم الفضائية — Vx");
            headerTitle.setTextSize(22f);
            headerTitle.setTypeface(null, Typeface.BOLD);
            headerTitle.setTextColor(redGlowAccent);
            headerTitle.setGravity(Gravity.START);
            container.addView(headerTitle, new LinearLayout.LayoutParams(-1, -2));

            TextView headerSub = new TextView(activity);
            headerSub.setText("مركز إدارة الحظر الصارم، الاستراحات، والجمل والمجالات المحظورة");
            headerSub.setTextSize(13f);
            headerSub.setTextColor(secondaryColor);
            LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(-1, -2);
            subParams.setMargins(0, dp(activity, 4), 0, dp(activity, 16));
            container.addView(headerSub, subParams);

            // Card 1: اختيار التطبيقات المحظورة
            container.addView(createCosmicCard(activity,
                    "اختيار التطبيقات",
                    "تحديد التطبيقات المحظورة لمنع التشتت أثناء الجلسة",
                    "فتح القائمة",
                    redGlowAccent,
                    v -> openScreen(activity, "com.vx.app.selection.AppSelectionActivity")
            ));

            // Card 2: اختيار المواقع المحظورة
            container.addView(createCosmicCard(activity,
                    "اختيار المواقع",
                    "حظر المواقع الإلكترونية والنطاقات غير المرغوب فيها",
                    "فتح القائمة",
                    emeraldAccent,
                    v -> openScreen(activity, "com.vx.app.selection.WebsiteSelectionActivity")
            ));

            // Card 3: اختيار الجمل والكلمات والإعدادات المحظورة
            container.addView(createCosmicCard(activity,
                    "الجمل والإعدادات المحظورة",
                    "تخصيص الكلمات والجمل وعناوين الإعدادات المحظورة ديناميكياً",
                    "تعديل القائمة",
                    purpleAccent,
                    v -> activity.startActivity(new Intent(activity, DynamicLabelsActivity.class))
            ));

            // Card 4: اختيار مدة الحظر الصارم
            container.addView(createCosmicCard(activity,
                    "مدة الحظر الصارم",
                    "تحديد وقت الحظر الصارم وبدء الجلسة فوراً بدون تعقيد",
                    "بدء مؤقت الحظر",
                    amberAccent,
                    v -> openScreen(activity, "com.vx.app.patch.DurationActivity")
            ));

            // Card 5: اختيار الفترة بين استراحات 1 دقيقة
            LinearLayout breakCard = new LinearLayout(activity);
            breakCard.setOrientation(LinearLayout.VERTICAL);
            breakCard.setPadding(dp(activity, 16), dp(activity, 16), dp(activity, 16), dp(activity, 16));

            GradientDrawable breakBg = new GradientDrawable();
            breakBg.setColor(cardBgColor);
            breakBg.setCornerRadius(dp(activity, 16));
            breakBg.setStroke(dp(activity, 2), 0xFF831843); // Meteor Crimson border glow
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
            minus.setBackground(createButtonBg(0xFF1F2937, dp(activity, 10)));
            row.addView(minus, new LinearLayout.LayoutParams(dp(activity, 52), dp(activity, 48)));

            TextView valueText = new TextView(activity);
            valueText.setTextSize(16f);
            valueText.setTypeface(null, Typeface.BOLD);
            valueText.setTextColor(cyanAccent);
            valueText.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams valParams = new LinearLayout.LayoutParams(0, -2, 1f);
            valParams.setMargins(dp(activity, 8), 0, dp(activity, 8), 0);
            row.addView(valueText, valParams);

            Button plus = new Button(activity);
            plus.setText("+");
            plus.setTextSize(22f);
            plus.setTextColor(textColor);
            plus.setBackground(createButtonBg(0xFF1F2937, dp(activity, 10)));
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

            // Card 6: صلاحيات التطبيق (Device Admin, Accessibility, Notifications)
            container.addView(createPermissionsCard(activity, redGlowAccent, cyanAccent, purpleAccent));

            scrollView.addView(container, new ViewGroup.LayoutParams(-1, -2));

            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(-1, -1);
            activity.addContentView(scrollView, layoutParams);

        } catch (Throwable ignored) {
            // Container safeguard
        }
    }

    private static View createCosmicCard(Activity activity, String title, String desc, String buttonText, int accentColor, View.OnClickListener listener) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(activity, 16), dp(activity, 16), dp(activity, 16), dp(activity, 16));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF131B2E);
        bg.setCornerRadius(dp(activity, 16));
        bg.setStroke(dp(activity, 1), 0xFF1E293B);
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

    private static View createPermissionsCard(Activity activity, int redColor, int cyanColor, int purpleColor) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(activity, 16), dp(activity, 16), dp(activity, 16), dp(activity, 16));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF131B2E);
        bg.setCornerRadius(dp(activity, 16));
        bg.setStroke(dp(activity, 2), 0xFF4338CA); // Indigo Glow Border
        card.setBackground(bg);

        TextView tvTitle = new TextView(activity);
        tvTitle.setText("صلاحيات التطبيق والحماية");
        tvTitle.setTextSize(16f);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(0xFFFFFFFF);
        card.addView(tvTitle, new LinearLayout.LayoutParams(-1, -2));

        TextView tvDesc = new TextView(activity);
        tvDesc.setText("عرض وإدارة الصلاحيات الأساسية لضمان عمل الحظر بدون انقطاع:");
        tvDesc.setTextSize(12f);
        tvDesc.setTextColor(0xFF94A3B8);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(-1, -2);
        descParams.setMargins(0, dp(activity, 4), 0, dp(activity, 12));
        card.addView(tvDesc, descParams);

        // Permission Button 1: Device Admin
        Button btnAdmin = createSmallButton(activity, "مشرف الجهاز (Device Admin)", redColor, v -> {
            try {
                Intent intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
                activity.startActivity(intent);
            } catch (Throwable t) {
                Toast.makeText(activity, "تعديل إعدادات الحماية والأدمن", Toast.LENGTH_SHORT).show();
            }
        });
        card.addView(btnAdmin);

        // Permission Button 2: Accessibility
        Button btnAccess = createSmallButton(activity, "إمكانية الوصول (Accessibility)", cyanColor, v -> {
            try {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                activity.startActivity(intent);
            } catch (Throwable t) {
                Toast.makeText(activity, "فتح إعدادات الوصول", Toast.LENGTH_SHORT).show();
            }
        });
        card.addView(btnAccess);

        // Permission Button 3: Notifications
        Button btnNotify = createSmallButton(activity, "الإشعارات (Notifications)", purpleColor, v -> {
            try {
                Intent intent = new Intent();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                    intent.putExtra(Settings.EXTRA_APP_PACKAGE, activity.getPackageName());
                } else {
                    intent.setAction("android.settings.ACTION_ID_NOTIFICATION_SETTINGS");
                    intent.putExtra("app_package", activity.getPackageName());
                    intent.putExtra("app_uid", activity.getApplicationInfo().uid);
                }
                activity.startActivity(intent);
            } catch (Throwable t) {
                Toast.makeText(activity, "فتح إعدادات الإشعارات", Toast.LENGTH_SHORT).show();
            }
        });
        card.addView(btnNotify);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.setMargins(0, 0, 0, dp(activity, 16));
        card.setLayoutParams(cardParams);

        return card;
    }

    private static Button createSmallButton(Activity activity, String text, int color, View.OnClickListener listener) {
        Button btn = new Button(activity);
        btn.setText(text);
        btn.setAllCaps(false);
        btn.setTextSize(13f);
        btn.setTextColor(0xFFFFFFFF);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setBackground(createButtonBg(color, dp(activity, 8)));
        btn.setOnClickListener(listener);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(activity, 40));
        params.setMargins(0, 0, 0, dp(activity, 8));
        btn.setLayoutParams(params);
        return btn;
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
