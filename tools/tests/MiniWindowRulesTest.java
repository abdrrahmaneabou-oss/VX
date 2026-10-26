import com.v1f.app.patch.MiniWindowRules;

public final class MiniWindowRulesTest {
    private static void check(boolean expected, int w, int h, int sw, int sh) {
        boolean actual = MiniWindowRules.isMiniBounds(w,h,sw,sh);
        if (actual != expected) throw new AssertionError(
                w + "x" + h + " within " + sw + "x" + sh + ": " + actual);
    }
    public static void main(String[] args) {
        check(true, 590, 1150, 1080, 2400); // OEM Mini
        check(true, 1080, 1100, 1080, 2400); // split-screen
        check(false, 1080, 2400, 1080, 2400); // regular app
        check(false, 100, 200, 1080, 2400); // popup, not an app window
        check(false, 0, 1000, 1080, 2400);
        check(false, 1600, 1200, 1080, 2400);
        check(true, 650, 480, 1080, 2400);
        System.out.println("MiniWindowRules tests passed");
    }
}
