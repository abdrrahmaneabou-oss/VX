package com.v1f.app.patch;

/** Small deterministic state machine for the one-shot Settings return action. */
public final class SettingsReturnState {
    public static final int IDLE = 0;
    public static final int WAITING_FOR_EXIT = 1;
    public static final int WAITING_FOR_RETURN = 2;
    private static final String SETTINGS = "com.android.settings";
    private static final String SELF = "com.v1f.app";
    private int state = IDLE;

    public void arm() { state = WAITING_FOR_EXIT; }

    public void restore(int restored) {
        state = restored == WAITING_FOR_EXIT || restored == WAITING_FOR_RETURN
                ? restored : IDLE;
    }

    public void clear() { state = IDLE; }

    public int getState() { return state; }

    /**
     * Returns true exactly once, on the first observed Settings foreground
     * after an observed departure. Null and our own blocking overlay are not
     * evidence of a foreground transition.
     */
    public boolean accept(String foregroundPackage) {
        if (state == IDLE || foregroundPackage == null
                || foregroundPackage.startsWith(SELF)) {
            return false;
        }
        if (state == WAITING_FOR_EXIT) {
            if (!SETTINGS.equals(foregroundPackage)) {
                state = WAITING_FOR_RETURN;
            }
            return false;
        }
        if (SETTINGS.equals(foregroundPackage)) {
            state = IDLE;
            return true;
        }
        return false;
    }
}
