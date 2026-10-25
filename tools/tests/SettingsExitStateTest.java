import com.v1f.app.patch.SettingsExitState;

public final class SettingsExitStateTest {
    private static void check(boolean actual, boolean expected, String name) {
        if (actual != expected) throw new AssertionError(name + ": " + actual);
    }

    public static void main(String[] args) {
        SettingsExitState state = new SettingsExitState();

        check(state.observe(false, "launcher", 0L), false, "no Settings visit");
        check(state.observe(false, "com.android.settings", 100L), false, "user enters Settings");
        check(state.observe(false, null, 200L), false, "unknown snapshot preserves visit");
        check(state.observe(false, "launcher", 300L), true, "user departure detected");
        check(state.observe(false, "launcher", 400L), false, "one departure only");

        // An automatic sequence can open Settings twice and then leave it
        // with three BACK commands; none of those transitions may retrigger.
        check(state.observe(true, null, 500L), false, "automatic sequence begins");
        check(state.observe(true, "com.android.settings", 750L), false, "automatic first open");
        check(state.observe(true, "com.android.settings", 1000L), false, "automatic second open");
        check(state.observe(true, "launcher", 2000L), false, "automatic back leaves Settings");
        check(state.observe(false, "launcher", 2700L), false, "late BACK event after operation");
        check(state.observe(false, "com.android.settings", 2800L), false, "late Settings transition");
        check(state.observe(false, "launcher", 3000L), false, "unstable exit is suppressed");
        check(state.observe(false, "launcher", 3500L), false, "no infinite loop");

        // After stability, normal user departures must work again.
        check(state.observe(false, "com.android.settings", 4000L), false, "new visit");
        check(state.observe(false, "com.android.settings", 4499L), false, "not stable enough");
        check(state.observe(false, "com.android.settings", 4500L), false, "visit rearmed");
        check(state.observe(false, "other.app", 4600L), true, "new user app switch");
        check(state.observe(false, "launcher", 4700L), false, "no retrigger");

        // If BACK stops in Settings, the next real user departure still works
        // once the post-navigation stable-focus guard has expired.
        check(state.observe(true, "com.android.settings", 5000L), false, "second sequence");
        check(state.observe(false, "com.android.settings", 6000L), false, "stable check start");
        check(state.observe(false, "com.android.settings", 6500L), false, "stable check done");
        check(state.observe(false, "launcher", 6600L), true, "user leaves remaining Settings screen");

        state.observe(false, "com.android.settings", 7000L);
        state.clear();
        check(state.observe(false, "launcher", 7100L), false, "session or service teardown");
        state.observe(false, "com.android.settings", 7200L);
        check(state.observe(false, "#screen-inactive", 7300L), true, "screen lock");

        System.out.println("SettingsExitState tests passed");
    }
}
