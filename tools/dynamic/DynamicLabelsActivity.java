package com.v1f.app.patch;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.LinkedHashSet;

public final class DynamicLabelsActivity extends Activity {
    private EditText editor;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        int cardBgColor = 0xFF1E2638;
        int borderColor = 0xFF334155;
        int primaryAccent = 0xFF8B5CF6; // Purple accent
        int dangerAccent = 0xFFEF4444;  // Red accent
        int textColor = 0xFFFFFFFF;
        int secondaryColor = 0xFF94A3B8;

        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(0xFF0F172A); // Dark slate background

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        root.setPadding(pad, pad, pad, pad);

        // Header Title
        TextView title = new TextView(this);
        title.setText("تخصيص قائمة الجمل والإعدادات المحظورة");
        title.setTextSize(20f);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(textColor);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView help = new TextView(this);
        help.setText("أدخل كل جملة أو عنوان إعداد في سطر مستقل لحظره تلقائياً عند الاكتشاف.");
        help.setTextSize(13f);
        help.setTextColor(secondaryColor);
        LinearLayout.LayoutParams helpParams = new LinearLayout.LayoutParams(-1, -2);
        helpParams.setMargins(0, dp(6), 0, dp(16));
        root.addView(help, helpParams);

        // Card Wrapper for Editor
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(cardBgColor);
        cardBg.setCornerRadius(dp(16));
        cardBg.setStroke(dp(1), borderColor);
        card.setBackground(cardBg);

        editor = new EditText(this);
        editor.setGravity(Gravity.TOP | Gravity.START);
        editor.setSingleLine(false);
        editor.setTextSize(14f);
        editor.setTextColor(textColor);
        editor.setHintTextColor(secondaryColor);
        editor.setHint("أدخل الجمل المحظورة هنا...");
        editor.setBackground(null);
        editor.setMinLines(10);
        editor.setText(join(DynamicLabelsStore.load(this)));
        card.addView(editor, new LinearLayout.LayoutParams(-1, dp(240)));

        LinearLayout.LayoutParams cardLayoutParams = new LinearLayout.LayoutParams(-1, -2);
        cardLayoutParams.setMargins(0, 0, 0, dp(20));
        root.addView(card, cardLayoutParams);

        // Save Button
        Button save = new Button(this);
        save.setText("حفظ التغييرات وتفعيل القائمة");
        save.setAllCaps(false);
        save.setTextSize(15f);
        save.setTextColor(textColor);
        save.setTypeface(null, Typeface.BOLD);
        save.setBackground(createButtonBg(primaryAccent, dp(12)));

        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(-1, dp(48));
        saveParams.setMargins(0, 0, 0, dp(12));
        root.addView(save, saveParams);

        // Reset Button
        Button reset = new Button(this);
        reset.setText("استعادة القائمة الافتراضية");
        reset.setAllCaps(false);
        reset.setTextSize(14f);
        reset.setTextColor(textColor);
        reset.setBackground(createButtonBg(0xFF374151, dp(12)));

        LinearLayout.LayoutParams resetParams = new LinearLayout.LayoutParams(-1, dp(44));
        root.addView(reset, resetParams);

        save.setOnClickListener(v -> {
            try {
                LinkedHashSet<String> values = parse(editor.getText().toString());
                DynamicLabelsStore.save(this, values);
                editor.setText(join(values));
                editor.setSelection(editor.length());
                Toast.makeText(this, "تم حفظ القائمة وتفعيل الحظر بنجاح", Toast.LENGTH_SHORT).show();
            } catch (Throwable error) {
                Toast.makeText(this, "تعذر حفظ القائمة: " + safeMessage(error), Toast.LENGTH_LONG).show();
            }
        });

        reset.setOnClickListener(v -> {
            try {
                DynamicLabelsStore.reset(this);
                editor.setText(join(DynamicLabelsStore.load(this)));
                editor.setSelection(editor.length());
                Toast.makeText(this, "تمت استعادة القائمة الافتراضية بنجاح", Toast.LENGTH_SHORT).show();
            } catch (Throwable error) {
                Toast.makeText(this, "تعذر استعادة القائمة: " + safeMessage(error), Toast.LENGTH_LONG).show();
            }
        });

        scrollView.addView(root, new ViewGroup.LayoutParams(-1, -2));
        setContentView(scrollView);
    }

    private LinkedHashSet<String> parse(String text) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String line : text.split("\\r?\\n")) {
            String value = line.trim();
            if (!TextUtils.isEmpty(value)) values.add(value);
        }
        return values;
    }

    private String join(Iterable<String> values) {
        StringBuilder out = new StringBuilder();
        for (String value : values) {
            if (out.length() > 0) out.append('\n');
            out.append(value);
        }
        return out.toString();
    }

    private String safeMessage(Throwable error) {
        String message = error == null ? null : error.getMessage();
        return TextUtils.isEmpty(message) ? "خطأ غير معروف" : message;
    }

    private GradientDrawable createButtonBg(int color, float radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
