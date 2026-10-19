package com.v1f.app.patch;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Edits only hourly exemptions. Leaving without Save discards the draft. */
public final class FullBlockExemptionsActivity extends Activity {
    private final List<String> packages = new ArrayList<>();
    private Set<String> draft;
    private ListView list;
    private Button save;
    private TextView status;
    private boolean loaded;

    public static void openFromCompose() {
        Activity host = DynamicLabelsEntry.getHost();
        if (host != null && !host.isFinishing() && !host.isDestroyed())
            host.startActivity(new Intent(host, FullBlockExemptionsActivity.class));
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        draft = FullBlockSettings.load(this);
        if (state != null && state.containsKey("draft"))
            draft = new HashSet<>(state.getStringArrayList("draft"));
        int bg = Color.rgb(10, 14, 31);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setBackgroundColor(bg);
        int pad = (int)(18 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);
        TextView title = text("استثناءات الحظر الشامل", 23);
        root.addView(title);
        root.addView(text("ألغِ تحديد ما تريد إعفاءه، ثم اضغط حفظ. التطبيقات المحددة يشملها الحظر الشامل. هذه القائمة مستقلة عن الحظر العادي.", 15));
        root.addView(text("تظهر هنا التطبيقات التي يشملها الحظر الشامل؛ تطبيق الهاتف يبقى مستثنى تلقائيًا.", 13));
        status = text("جارٍ تحميل التطبيقات…", 14);
        root.addView(status);
        list = new ListView(this);
        list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1));
        list.setOnItemClickListener((parent, view, position, id) -> {
            String pkg = packages.get(position);
            if (list.isItemChecked(position)) draft.remove(pkg);
            else draft.add(pkg);
            showCount();
        });
        save = new Button(this);
        save.setText("حفظ");
        save.setTextColor(Color.WHITE);
        GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.rgb(91, 110, 238), Color.rgb(144, 117, 252)});
        gradient.setCornerRadius(pad);
        save.setBackground(gradient);
        save.setEnabled(false);
        save.setOnClickListener(v -> {
            if (!loaded) return;
            if (FullBlockSettings.save(this, draft)) {
                Toast.makeText(this, "تم حفظ الاستثناءات", Toast.LENGTH_SHORT).show();
                finish();
            } else Toast.makeText(this, "تعذر الحفظ، حاول مرة أخرى", Toast.LENGTH_LONG).show();
        });
        root.addView(save, new LinearLayout.LayoutParams(-1, pad * 3));
        Button back = new Button(this);
        back.setText("رجوع دون حفظ");
        back.setOnClickListener(v -> finish());
        root.addView(back);
        setContentView(root);
        new Thread(this::loadApps, "hourly-exemptions-loader").start();
    }

    private TextView text(String value, int size) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextColor(Color.rgb(243, 246, 255));
        view.setTextSize(size);
        view.setPadding(0, 8, 0, 12);
        return view;
    }

    private void loadApps() {
        try {
            PackageManager pm = getPackageManager();
            List<ApplicationInfo> apps = new ArrayList<>();
            for (ApplicationInfo info : pm.getInstalledApplications(0))
                if (RandomFullBlock.eligiblePackage(this, info.packageName)) apps.add(info);
            Collections.sort(apps, new ApplicationInfo.DisplayNameComparator(pm));
            List<String> names = new ArrayList<>();
            List<String> ids = new ArrayList<>();
            for (ApplicationInfo info : apps) {
                names.add(info.loadLabel(pm) + "\n" + info.packageName);
                ids.add(info.packageName);
            }
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                packages.addAll(ids);
                list.setAdapter(new ArrayAdapter<String>(this,
                        android.R.layout.simple_list_item_multiple_choice, names) {
                    @Override public View getView(int position, View recycled, android.view.ViewGroup parent) {
                        TextView row = (TextView)super.getView(position, recycled, parent);
                        row.setTextColor(Color.WHITE);
                        row.setTextSize(15);
                        return row;
                    }
                });
                for (int i = 0; i < packages.size(); i++)
                    list.setItemChecked(i, FullBlockRules.included(draft, packages.get(i)));
                loaded = true;
                save.setEnabled(true);
                showCount();
            });
        } catch (Exception error) {
            runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed()) status.setText("تعذر تحميل القائمة. ارجع وأعد المحاولة.");
            });
        }
    }

    private void showCount() {
        int count = 0;
        for (String pkg : packages) if (draft.contains(pkg)) count++;
        status.setText("التطبيقات المستثناة: " + count);
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putStringArrayList("draft", new ArrayList<>(draft));
        super.onSaveInstanceState(state);
    }
}
