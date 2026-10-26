package com.v1f.app.patch;

/** Pure, testable break/session decision table. */
public final class BreakPauseRules {
    private BreakPauseRules() {}

    public static boolean protectionEnabled(boolean sessionRunning, boolean breakAllowsAll) {
        return sessionRunning && !breakAllowsAll;
    }

    public static boolean breakActive(boolean sessionRunning, boolean breakAllowsAll) {
        return sessionRunning && breakAllowsAll;
    }

    public static boolean closeLock(boolean sessionRunning, boolean breakAllowsAll) {
        return !sessionRunning || breakAllowsAll;
    }
}
