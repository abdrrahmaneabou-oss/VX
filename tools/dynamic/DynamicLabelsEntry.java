package com.v1f.app.patch;

import android.app.Activity;
import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;

/** Keep the original protection-list entry above the Compose content layer. */
public final class DynamicLabelsEntry {
    private DynamicLabelsEntry() {}

    public static void attach(Activity activity) {
        try {
            // Defer insertion until the activity's initial UI has been attached.
            // A Compose setContent() during startup can otherwise cover an early
            // addContentView() button with its full-screen content surface.
            View decor = activity.getWindow().getDecorView();
            decor.post(() -> {
                if (activity.isFinishing() || activity.isDestroyed()) return;
                try {
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
                    button.bringToFront();
                } catch (Throwable error) {
                    android.util.Log.e("V2F-ProtectionList", "Unable to attach protection list", error);
                }
            });
        } catch (Throwable error) {
            android.util.Log.e("V2F-ProtectionList", "Unable to schedule protection list", error);
        }
    }
}
