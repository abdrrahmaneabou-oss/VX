package com.v1f.app.patch;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.LinkedHashSet;

public final class DynamicLabelsActivity extends Activity {
    private static final int BG = Color.rgb(10, 14, 31);
    private static final int PANEL = Color.rgb(23, 30, 52);
    private static final int INK = Color.rgb(243, 246, 255);
    private static final int MUTED = Color.rgb(165, 180, 208);
    private static final int CYAN = Color.rgb(91, 210, 238);
    private static final int PURPLE = Color.rgb(144, 117, 252);
    private EditText editor;
    private TextView intervalValue, lockStatus, countLabel;
    private Button minus, plus, save, reset;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean locked;
    private final Runnable policyRefresh = new Runnable() {
        @Override public void run() {
            refreshPolicy();
            if (!isFinishing()) handler.postDelayed(this, 500L);
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(0);

        LinearLayout root = column();
        root.setBackgroundColor(BG);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setPadding(dp(20), dp(14), dp(20), dp(18));

        LinearLayout header = row();
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = label("‹", 36, INK, true);
        back.setGravity(Gravity.CENTER);
        back.setBackground(shape(PANEL, 18, Color.rgb(59, 72, 107)));
        header.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));
        back.setOnClickListener(v -> finish());
        LinearLayout heading = column();
        heading.setPadding(dp(12), 0, dp(12), 0);
        TextView eyebrow = label("V1F  /  CELESTIAL CONTROL", 11, CYAN, true);
        TextView title = label("قائمة الحماية", 27, INK, true);
        heading.addView(eyebrow);
        heading.addView(title);
        header.addView(heading, new LinearLayout.LayoutParams(0, -2, 1f));
        root.addView(header, new LinearLayout.LayoutParams(-1, dp(68)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout content = column();
        content.setPadding(0, dp(12), 0, dp(20));

        LinearLayout statusCard = card();
        TextView statusHeading = label("حالة التحكم", 12, CYAN, true);
        statusCard.addView(statusHeading);
        lockStatus = label("", 15, INK, true);
        LinearLayout.LayoutParams statusMargin = new LinearLayout.LayoutParams(-1, -2);
        statusMargin.topMargin = dp(8);
        statusCard.addView(lockStatus, statusMargin);
        TextView statusHelp = label("يمكن تغيير الإعدادات فقط عندما تسمح سياسة الحماية بذلك.", 12, MUTED, false);
        LinearLayout.LayoutParams statusHelpMargin = new LinearLayout.LayoutParams(-1, -2);
        statusHelpMargin.topMargin = dp(8);
        statusCard.addView(statusHelp, statusHelpMargin);
        content.addView(statusCard, section());

        LinearLayout intervalCard = card();
        intervalCard.addView(label("✦  جدول الاستراحة", 20, INK, true));
        TextView intervalSubtitle = label("المدة الفاصلة بين فترات الاستراحة", 13, MUTED, false);
        intervalSubtitle.setPadding(0, dp(7), 0, dp(16));
        intervalCard.addView(intervalSubtitle);

        LinearLayout intervalRow = row();
        intervalRow.setGravity(Gravity.CENTER_VERTICAL);
        minus = button("−", false);
        intervalRow.addView(minus, new LinearLayout.LayoutParams(dp(56), dp(56)));
        LinearLayout intervalMiddle = column();
        intervalMiddle.setGravity(Gravity.CENTER);
        intervalValue = label("", 25, INK, true);
        intervalValue.setGravity(Gravity.CENTER);
        intervalMiddle.addView(intervalValue);
        TextView intervalTag = label("بين الاستراحات", 11, MUTED, false);
        intervalTag.setGravity(Gravity.CENTER);
        intervalMiddle.addView(intervalTag);
        intervalRow.addView(intervalMiddle, new LinearLayout.LayoutParams(0, dp(62), 1f));
        plus = button("+", false);
        intervalRow.addView(plus, new LinearLayout.LayoutParams(dp(56), dp(56)));
        intervalCard.addView(intervalRow);
        TextView intervalHelp = label("من ساعة إلى 24 ساعة  •  يُحفظ التغيير فورًا", 12, MUTED, false);
        intervalHelp.setPadding(0, dp(16), 0, 0);
        intervalCard.addView(intervalHelp);
        content.addView(intervalCard, section());

        LinearLayout labelsCard = card();
        LinearLayout labelsHeader = row();
        labelsHeader.setGravity(Gravity.CENTER_VERTICAL);
        TextView labelsTitle = label("✧  الجمل المحظورة", 20, INK, true);
        labelsHeader.addView(labelsTitle, new LinearLayout.LayoutParams(0, -2, 1f));
        countLabel = label("", 12, CYAN, true);
        countLabel.setPadding(dp(10), dp(6), dp(10), dp(6));
        countLabel.setBackground(shape(Color.rgb(26, 58, 80), 14, Color.rgb(46, 115, 143)));
        labelsHeader.addView(countLabel);
        labelsCard.addView(labelsHeader);
        TextView labelsHelp = label("اكتب كل جملة في سطر مستقل. تُستخدم هذه القائمة لحماية إعدادات الجهاز.", 13, MUTED, false);
        labelsHelp.setPadding(0, dp(9), 0, dp(14));
        labelsCard.addView(labelsHelp);

        editor = new EditText(this);
        editor.setTextColor(INK);
        editor.setHintTextColor(MUTED);
        editor.setTextSize(15f);
        editor.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        editor.setGravity(Gravity.TOP | Gravity.START);
        editor.setSingleLine(false);
        editor.setMinLines(9);
        editor.setPadding(dp(15), dp(16), dp(15), dp(16));
        editor.setBackground(shape(Color.rgb(13, 20, 39), 17, Color.rgb(55, 79, 112)));
        editor.setHint("أضف الجمل التي تريد حظرها...");
        editor.setText(join(DynamicLabelsStore.load(this)));
        editor.setHorizontallyScrolling(false);
        labelsCard.addView(editor, new LinearLayout.LayoutParams(-1, dp(285)));
        TextView editorTip = label("يُحفظ النص عند الضغط على زر الحفظ أدناه", 12, MUTED, false);
        editorTip.setPadding(0, dp(11), 0, 0);
        labelsCard.addView(editorTip);
        content.addView(labelsCard, section());

        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout actions = column();
        actions.setPadding(0, dp(10), 0, 0);
        save = button("✓   حفظ تغييرات الحماية", true);
        actions.addView(save, new LinearLayout.LayoutParams(-1, dp(56)));
        reset = button("↺   استعادة القائمة الافتراضية", false);
        LinearLayout.LayoutParams resetMargin = new LinearLayout.LayoutParams(-1, dp(50));
        resetMargin.topMargin = dp(9);
        actions.addView(reset, resetMargin);
        root.addView(actions, new LinearLayout.LayoutParams(-1, -2));

        minus.setOnClickListener(v -> {
            if (!requireEditable()) return;
            BreakIntervalStore.adjustHours(-1);
            refreshInterval();
            refreshPolicy();
        });
        plus.setOnClickListener(v -> {
            if (!requireEditable()) return;
            BreakIntervalStore.adjustHours(1);
            refreshInterval();
            refreshPolicy();
        });
        save.setOnClickListener(v -> {
            if (!requireEditable()) return;
            try {
                LinkedHashSet<String> values = parse(editor.getText().toString());
                DynamicLabelsStore.save(this, values);
                editor.setText(join(values));
                editor.setSelection(editor.length());
                updateCount();
                Toast.makeText(this, "تم حفظ القائمة وتفعيلها", Toast.LENGTH_SHORT).show();
            } catch (Throwable error) {
                Toast.makeText(this, "تعذر حفظ القائمة: " + safeMessage(error), Toast.LENGTH_LONG).show();
            }
            refreshPolicy();
        });
        reset.setOnClickListener(v -> {
            if (!requireEditable()) return;
            try {
                DynamicLabelsStore.reset(this);
                editor.setText(join(DynamicLabelsStore.load(this)));
                editor.setSelection(editor.length());
                updateCount();
                Toast.makeText(this, "تمت استعادة القائمة الافتراضية", Toast.LENGTH_SHORT).show();
            } catch (Throwable error) {
                Toast.makeText(this, "تعذر استعادة القائمة: " + safeMessage(error), Toast.LENGTH_LONG).show();
            }
            refreshPolicy();
        });
        setContentView(root);
        updateCount();
        refreshInterval();
        refreshPolicy();
    }

    private LinearLayout column() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        v.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        return v;
    }
    private LinearLayout row() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.HORIZONTAL);
        v.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        return v;
    }
    private LinearLayout card() {
        LinearLayout v = column();
        v.setPadding(dp(18), dp(18), dp(18), dp(18));
        v.setBackground(shape(PANEL, 22, Color.rgb(55, 76, 112)));
        return v;
    }
    private LinearLayout.LayoutParams section() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = dp(15);
        return p;
    }
    private TextView label(String text, int size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        v.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        v.setIncludeFontPadding(true);
        return v;
    }
    private Button button(String text, boolean primary) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(15f);
        b.setAllCaps(false);
        b.setTextColor(primary ? BG : INK);
        b.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        b.setBackground(primary ? gradient() : shape(Color.rgb(36, 46, 73), 16, Color.rgb(69, 86, 121)));
        b.setStateListAnimator(null);
        return b;
    }
    private GradientDrawable shape(int fill, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radius));
        d.setStroke(dp(1), stroke);
        return d;
    }
    private GradientDrawable gradient() {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{PURPLE, CYAN});
        d.setCornerRadius(dp(18));
        return d;
    }
    @Override protected void onResume() {
        super.onResume();
        handler.removeCallbacks(policyRefresh);
        handler.post(policyRefresh);
    }
    @Override protected void onPause() {
        handler.removeCallbacks(policyRefresh);
        super.onPause();
    }
    private boolean requireEditable() {
        if (StrictBlockPolicy.allowsAll()) return true;
        refreshPolicy();
        Toast.makeText(this, "الإعدادات مقفلة أثناء الحظر الصارم. انتظر الاستراحة أو أوقف الحظر.", Toast.LENGTH_LONG).show();
        return false;
    }
    private void refreshPolicy() {
        boolean canEdit = StrictBlockPolicy.allowsAll();
        locked = !canEdit;
        if (lockStatus != null) {
            lockStatus.setText(canEdit ? "●  التعديل متاح الآن" : "🔒  الحماية الصارمة نشطة — التعديل مقفل");
            lockStatus.setTextColor(canEdit ? CYAN : Color.rgb(255, 188, 131));
        }
        if (editor != null) editor.setEnabled(canEdit);
        if (minus != null) minus.setEnabled(canEdit && BreakIntervalStore.getHours() > 1);
        if (plus != null) plus.setEnabled(canEdit && BreakIntervalStore.getHours() < 24);
        if (save != null) save.setEnabled(canEdit);
        if (reset != null) reset.setEnabled(canEdit);
        if (editor != null && locked) {
            editor.setText(join(DynamicLabelsStore.load(this)));
            editor.setSelection(0);
        }
        updateCount();
        refreshInterval();
    }
    private void refreshInterval() {
        if (intervalValue != null) intervalValue.setText(BreakIntervalStore.getHours() + " ساعة");
    }
    private void updateCount() {
        if (countLabel != null && editor != null)
            countLabel.setText(parse(editor.getText().toString()).size() + " جملة");
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
    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
