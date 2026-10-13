package com.v1f.app.patch;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.LinkedHashSet;

public final class DynamicLabelsActivity extends Activity {
    private EditText editor;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("قائمة الحماية");
        title.setTextSize(24f);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView help = new TextView(this);
        help.setText("كل جملة في سطر مستقل. يمكنك إضافة سطر أو تعديله أو حذفه ثم الضغط على حفظ.");
        LinearLayout.LayoutParams helpParams = new LinearLayout.LayoutParams(-1, -2);
        helpParams.setMargins(0, dp(8), 0, dp(12));
        root.addView(help, helpParams);

        editor = new EditText(this);
        editor.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);
        editor.setSingleLine(false);
        editor.setText(join(DynamicLabelsStore.load(this)));
        root.addView(editor, new LinearLayout.LayoutParams(-1, 0, 1f));

        Button save = new Button(this);
        save.setText("حفظ التغييرات");
        save.setAllCaps(false);
        root.addView(save, new LinearLayout.LayoutParams(-1, -2));

        Button reset = new Button(this);
        reset.setText("استعادة القائمة الافتراضية");
        reset.setAllCaps(false);
        root.addView(reset, new LinearLayout.LayoutParams(-1, -2));

        save.setOnClickListener(v -> {
            LinkedHashSet<String> values = parse(editor.getText().toString());
            DynamicLabelsStore.save(this, values);
            editor.setText(join(values));
            Toast.makeText(this, "تم حفظ القائمة", Toast.LENGTH_SHORT).show();
        });

        reset.setOnClickListener(v -> {
            DynamicLabelsStore.reset(this);
            editor.setText(join(DynamicLabelsStore.load(this)));
            Toast.makeText(this, "تمت استعادة القائمة الافتراضية", Toast.LENGTH_SHORT).show();
        });

        setContentView(root);
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
