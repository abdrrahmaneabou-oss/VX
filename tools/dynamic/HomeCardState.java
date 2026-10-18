package com.v1f.app.patch;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.text.TextUtils;
import java.util.Locale;

public final class HomeCardState {
  private HomeCardState() {}
  public static boolean accessibilityReady() {
    Activity activity = DynamicLabelsEntry.getHost();
    if (activity == null) return false;
    try {
      String services = Settings.Secure.getString(activity.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
      if (Settings.Secure.getInt(activity.getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED, 0) != 1 || services == null) return false;
      for (String entry : services.split(":")) {
        if (entry.toLowerCase(Locale.ROOT).contains(activity.getPackageName().toLowerCase(Locale.ROOT) + "/")) return true;
      }
    } catch (Exception ignored) {}
    return false;
  }
  public static String permissionLabel() {
    return accessibilityReady() ? "الصلاحيات" : "🔴 الصلاحيات";
  }
  public static String durationLabel() {
    Activity activity = DynamicLabelsEntry.getHost();
    if (activity == null) return "◷ مدة الحظر";
    SharedPreferences p = activity.getSharedPreferences("v2f_duration_entry", Context.MODE_PRIVATE);
    int d = p.getInt("value0", 0), h = p.getInt("value1", 0), m = p.getInt("value2", 0);
    if (d == 0 && h == 0 && m == 0) return "◷ مدة الحظر • غير محددة";
    return "◷ مدة الحظر • " + d + " يوم، " + h + " ساعة، " + m + " دقيقة";
  }
}
