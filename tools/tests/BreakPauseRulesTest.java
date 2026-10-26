import com.v1f.app.patch.BreakPauseRules;

public final class BreakPauseRulesTest {
    private static void check(boolean expected, boolean actual, String label) {
        if (expected != actual) throw new AssertionError(label);
    }
    public static void main(String[] args) {
        check(false, BreakPauseRules.protectionEnabled(false,false), "no session");
        check(false, BreakPauseRules.protectionEnabled(false,true), "no session + stale break");
        check(true, BreakPauseRules.protectionEnabled(true,false), "active session");
        check(false, BreakPauseRules.protectionEnabled(true,true), "break disables guards");
        check(true, BreakPauseRules.breakActive(true,true), "break state");
        check(false, BreakPauseRules.breakActive(false,true), "no session means no active break");
        check(true, BreakPauseRules.closeLock(true,true), "dismiss lock during break");
        check(true, BreakPauseRules.closeLock(false,false), "dismiss lock after session");
        check(false, BreakPauseRules.closeLock(true,false), "keep lock during active session");
        System.out.println("BreakPauseRules tests passed");
    }
}
