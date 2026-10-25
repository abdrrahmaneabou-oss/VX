import com.v1f.app.patch.SettingsExitState;
public final class SettingsExitStateTest {
    private static void check(boolean actual, boolean expected, String name) {
        if (actual != expected) throw new AssertionError(name);
    }
    public static void main(String[] args) {
        SettingsExitState state = new SettingsExitState();
        check(state.observe(false, "launcher"), false, "no Settings visit");
        check(state.observe(false, "com.android.settings"), false, "enter Settings");
        check(state.observe(false, null), false, "unknown snapshot");
        check(state.observe(false, "launcher"), true, "user exits Settings");
        check(state.observe(false, "launcher"), false, "one departure only");
        check(state.observe(true, "com.android.settings"), false, "automatic reopen");
        check(state.observe(true, "launcher"), false, "automatic BACK");
        check(state.observe(false, "launcher"), false, "no recursive departure");
        check(state.observe(false, "com.android.settings"), false, "new user visit");
        check(state.observe(false, "other.app"), true, "app switch");
        state.observe(false, "com.android.settings");
        state.clear();
        check(state.observe(false, "launcher"), false, "service teardown");
        state.observe(false, "com.android.settings");
        check(state.observe(false, "#screen-inactive"), true, "screen lock");
        System.out.println("SettingsExitState tests passed");
    }
}
