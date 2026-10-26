import com.v1f.app.patch.BlockedBackSequencePlan;

public final class BlockedBackSequencePlanTest {
    private static void check(long expected, long actual) {
        if (expected != actual) throw new AssertionError(
                "Expected " + expected + " but was " + actual);
    }

    public static void main(String[] args) {
        check(5, BlockedBackSequencePlan.BACK_COUNT);
        check(250, BlockedBackSequencePlan.INTERVAL_MS);
        check(0, BlockedBackSequencePlan.backOffsetMs(0));
        check(250, BlockedBackSequencePlan.backOffsetMs(1));
        check(500, BlockedBackSequencePlan.backOffsetMs(2));
        check(750, BlockedBackSequencePlan.backOffsetMs(3));
        check(1000, BlockedBackSequencePlan.backOffsetMs(4));
        check(1250, BlockedBackSequencePlan.lockOffsetMs());
        for (int i=1; i<BlockedBackSequencePlan.BACK_COUNT; i++)
            check(250, BlockedBackSequencePlan.backOffsetMs(i)
                       - BlockedBackSequencePlan.backOffsetMs(i-1));
        boolean invalid = false;
        try { BlockedBackSequencePlan.backOffsetMs(5); }
        catch (IllegalArgumentException expected) { invalid = true; }
        if (!invalid) throw new AssertionError("Invalid sixth BACK index accepted");
        System.out.println("BlockedBackSequencePlan tests passed");
    }
}
