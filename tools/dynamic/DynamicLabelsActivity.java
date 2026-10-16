package com.v1f.app.patch;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.LinkedHashSet;

public final class DynamicLabelsActivity extends Activity {
    private EditText editor;
    private TextView intervalValue;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("إعدادات V2F");
        title.setTextSize(24f);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView help = new TextView(this);
        help.setText("فترة الاستراحة وقائمة الجمل المحظورة. كل جملة في سطر مستقل.");
        LinearLayout.LayoutParams helpParams = new LinearLayout.LayoutParams(-1, -2);
        helpParams.setMargins(0, dp(8), 0, dp(10));
        root.addView(help, helpParams);

        TextView intervalTitle = new TextView(this);
        intervalTitle.setText("الفاصل بين فترات الاستراحة");
        intervalTitle.setTextSize(17f);
        root.addView(intervalTitle, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout intervalRow = new LinearLayout(this);
        intervalRow.setOrientation(LinearLayout.HORIZONTAL);
        intervalRow.setGravity(Gravity.CENTER_VERTICAL);

        Button minus = new Button(this);
        minus.setText("− ساعة");
        minus.setAllCaps(false);
        intervalRow.addView(minus, new LinearLayout.LayoutParams(0, -2, 1f));

        intervalValue = new TextView(this);
        intervalValue.setTextSize(20f);
        intervalValue.setGravity(Gravity.CENTER);
        intervalRow.addView(intervalValue, new LinearLayout.LayoutParams(0, -2, 1f));

        Button plus = new Button(this);
        plus.setText("+ ساعة");
        plus.setAllCaps(false);
        intervalRow.addView(plus, new LinearLayout.LayoutParams(0, -2, 1f));
        root.addView(intervalRow, new LinearLayout.LayoutParams(-1, -2));

        TextView intervalHelp = new TextView(this);
        intervalHelp.setText("من ساعة إلى 24 ساعة. التغيير يُحفظ فورًا.");
        LinearLayout.LayoutParams intervalHelpParams = new LinearLayout.LayoutParams(-1, -2);
        intervalHelpParams.setMargins(0, 0, 0, dp(12));
        root.addView(intervalHelp, intervalHelpParams);
        refreshInterval();

        minus.setOnClickListener(v -> {
            BreakIntervalStore.adjustHours(-1);
            refreshInterval();
        });
        plus.setOnClickListener(v -> {
            BreakIntervalStore.adjustHours(1);
            refreshInterval();
        });

        TextView labelsTitle = new TextView(this);
        labelsTitle.setText("قائمة الجمل المحظورة");
        labelsTitle.setTextSize(17f);
        root.addView(labelsTitle, new LinearLayout.LayoutParams(-1, -2));

        editor = new EditText(this);
        editor.setGravity(Gravity.TOP | Gravity.START);
        editor.setSingleLine(false);
        editor.setText(join(DynamicLabelsStore.load(this)));
        root.addView(editor, new LinearLayout.LayoutParams(-1, 0, 1f));

        Button save = new Button(this);
        save.setText("حفظ تغييرات قائمة الحماية");
        save.setAllCaps(false);
        root.addView(save, new LinearLayout.LayoutParams(-1, -2));

        Button reset = new Button(this);
        reset.setText("استعادة القائمة الافتراضية");
        reset.setAllCaps(false);
        root.addView(reset, new LinearLayout.LayoutParams(-1, -2));

        save.setOnClickListener(v -> {
            try {
                LinkedHashSet<String> values = parse(editor.getText().toString());
                DynamicLabelsStore.save(this, values);
                editor.setText(join(values));
                editor.setSelection(editor.length());
                Toast.makeText(this, "تم حفظ القائمة وتفعيلها", Toast.LENGTH_SHORT).show();
            } catch (Throwable error) {
                Toast.makeText(this, "تعذر حفظ القائمة: " + safeMessage(error), Toast.LENGTH_LONG).show();
            }
        });

        reset.setOnClickListener(v -> {
            try {
                DynamicLabelsStore.reset(this);
                editor.setText(join(DynamicLabelsStore.load(this)));
                editor.setSelection(editor.length());
                Toast.makeText(this, "تمت استعادة القائمة الافتراضية", Toast.LENGTH_SHORT).show();
            } catch (Throwable error) {
                Toast.makeText(this, "تعذر استعادة القائمة: " + safeMessage(error), Toast.LENGTH_LONG).show();
            }
        });

        setContentView(root);
    }

    private void refreshInterval() {
        if (intervalValue != null) {
            intervalValue.setText(BreakIntervalStore.getHours() + " ساعة");
        }
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
