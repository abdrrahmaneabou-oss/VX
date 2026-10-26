package com.v1f.app.patch;

/**
 * Delays measured from the first BACK request (at t=0).
 * Five BACKs at 0, 250, 500, 750, 1000 ms; traditional lock at t=1250 ms.
 */
public final class BlockedBackSequencePlan {
    public static final int BACK_COUNT = 5;
    public static final long INTERVAL_MS = 250L;

    private BlockedBackSequencePlan() {}

    public static long backOffsetMs(int index) {
        if (index < 0 || index >= BACK_COUNT)
            throw new IllegalArgumentException("BACK index must be 0..4");
        return index * INTERVAL_MS;
    }

    public static long lockOffsetMs() {
        return BACK_COUNT * INTERVAL_MS;
    }
}
