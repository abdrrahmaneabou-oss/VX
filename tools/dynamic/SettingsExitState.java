package com.v1f.app.patch;

/** One departure per observed Settings visit; our own navigation cannot rearm it. */
public final class SettingsExitState {
    private boolean settingsSeen;
    public void clear() { settingsSeen = false; }
    public boolean observe(boolean ownNavigation, String foreground) {
        if (ownNavigation) {
            clear();
            return false;
        }
        if (foreground == null) return false;
        boolean settings = "com.android.settings".equals(foreground);
        boolean departed = settingsSeen && !settings;
        settingsSeen = settings;
        return departed;
    }
}
