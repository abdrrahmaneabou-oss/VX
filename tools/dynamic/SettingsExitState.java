package com.v1f.app.patch;

/**
 * Detect user departures only after a real Settings visit. Automatic navigation
 * disarms the detector until Settings is observed stably again, so delayed
 * global-BACK window events cannot cause an endless restart loop.
 */
public final class SettingsExitState {
    private static final String SETTINGS = "com.android.settings";
    private static final long SETTLE_MS = 500L;
    private boolean settingsSeen;
    private boolean requireStableReturn;
    private long stableSince = -1L;

    public void clear() {
        settingsSeen = false;
        requireStableReturn = false;
        stableSince = -1L;
    }

    public boolean observe(boolean ownNavigation, String foreground, long nowMs) {
        if (ownNavigation) {
            settingsSeen = false;
            requireStableReturn = true;
            stableSince = -1L;
            return false;
        }
        if (foreground == null) return false; // Unknown frames do not imply exit.

        boolean settings = SETTINGS.equals(foreground);
        if (requireStableReturn) {
            // After programmatic BACKs, ignore all leaving/arriving transitions
            // until a new, stable Settings foreground is verified.
            if (!settings) {
                stableSince = -1L;
                return false;
            }
            if (stableSince < 0L || nowMs < stableSince) {
                stableSince = nowMs;
                return false;
            }
            if (nowMs - stableSince < SETTLE_MS) return false;
            requireStableReturn = false;
            settingsSeen = true;
            return false;
        }

        boolean departed = settingsSeen && !settings;
        settingsSeen = settings;
        return departed;
    }
}
