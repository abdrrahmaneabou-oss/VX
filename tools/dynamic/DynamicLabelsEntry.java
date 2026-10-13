package com.v1f.app.patch;

import android.app.Activity;
import android.content.Intent;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;

public final class DynamicLabelsEntry {
    private DynamicLabelsEntry() {}

    public static void attach(Activity activity) {
        Button button = new Button(activity);
        button.setText("قائمة الحماية");
        button.setAllCaps(false);
        button.setAlpha(0.9f);
        button.setOnClickListener(v -> activity.startActivity(
                new Intent(activity, DynamicLabelsActivity.class)
        ));

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.END | Gravity.BOTTOM;
        int margin = Math.round(16 * activity.getResources().getDisplayMetrics().density);
        params.setMargins(margin, margin, margin, margin * 2);
        activity.addContentView(button, params);
    }
}
