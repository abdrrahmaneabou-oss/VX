import com.v1f.app.patch.SettingsReturnState;

public final class SettingsReturnStateTest {
    private static void assertThat(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
    }

    public static void main(String[] args) {
        SettingsReturnState state = new SettingsReturnState();
        state.arm();
        assertThat(!state.accept("com.android.settings"), "Initial Settings is not a return");
        assertThat(!state.accept("com.v1f.app"), "Our block window isn't departure");
        assertThat(!state.accept(null), "Missing foreground isn't departure");
        assertThat(!state.accept("com.android.systemui"), "Home should arm the return stage");
        assertThat(state.getState() == SettingsReturnState.WAITING_FOR_RETURN,
                "Must wait for Settings to return");
        assertThat(state.accept("com.android.settings"), "First Settings return triggers");
        assertThat(!state.accept("com.android.settings"), "Return pair must be one shot");

        // User reaches Home BEFORE the initial two BACK actions finish.
        state.arm();
        assertThat(!state.accept("com.android.launcher3"), "Home first");
        assertThat(state.accept("com.android.settings"), "Settings from background");

        // Repeated Settings content events while it never left aren't a return.
        state.arm();
        assertThat(!state.accept("com.android.settings"), "Still Settings");
        assertThat(!state.accept("com.android.settings"), "Still Settings again");
        assertThat(state.getState() == SettingsReturnState.WAITING_FOR_EXIT,
                "Do not consume pair while Settings is still visible");

        // Android recreates Accessibility while a return is pending.
        state.restore(SettingsReturnState.WAITING_FOR_RETURN);
        assertThat(!state.accept("com.v1f.app.patch"), "Ignore our overlay after restore");
        assertThat(state.accept("com.android.settings"), "Restore pending operation");
        state.restore(57);
        assertThat(state.getState() == SettingsReturnState.IDLE, "Discard corrupt state");

        System.out.println("Settings return: all state tests passed");
    }
}
