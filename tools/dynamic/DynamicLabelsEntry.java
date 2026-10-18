package com.v1f.app.patch;

import android.app.Activity;
import android.content.Intent;
import java.lang.ref.WeakReference;

/** Navigation host only: the protection-list button is rendered in native Compose. */
public final class DynamicLabelsEntry {
    private static WeakReference<Activity> host = new WeakReference<>(null);

    private DynamicLabelsEntry() {}

    public static void attach(Activity activity) {
        host = new WeakReference<>(activity);
    }

    public static Activity getHost() { return host.get(); }

    public static void openFromCompose() {
        Activity activity = host.get();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        activity.startActivity(new Intent(activity, DynamicLabelsActivity.class));
    }
}
