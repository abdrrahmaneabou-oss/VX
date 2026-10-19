import com.v1f.app.patch.FullBlockRules;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

public final class FullBlockRulesTest {
    private static void check(boolean ok, String detail) {
        if (!ok) throw new AssertionError(detail);
    }
    public static void main(String[] args) {
        String[] zones = {"UTC", "Europe/Moscow", "Asia/Kathmandu", "America/New_York"};
        for (String zone : zones) {
            TimeZone tz = TimeZone.getTimeZone(zone);
            for (int minutes : new int[]{1, 10, 15, 30, 45}) {
                ZonedDateTime hour = ZonedDateTime.of(2026, 10, 19, 14, 0, 0, 0, tz.toZoneId());
                long start = hour.plusMinutes(60 - minutes).toInstant().toEpochMilli();
                long end = hour.plusHours(1).toInstant().toEpochMilli();
                check(!FullBlockRules.windowActive(start - 1, tz, minutes), "before start " + zone);
                check(FullBlockRules.windowActive(start, tz, minutes), "exact start " + zone);
                check(FullBlockRules.windowActive(end - 1, tz, minutes), "last millisecond " + zone);
                check(!FullBlockRules.windowActive(end, tz, minutes), "next hour " + zone);
                check(FullBlockRules.remainingMillis(start, tz, minutes) == minutes * 60000L, "full duration");
                check(FullBlockRules.remainingMillis(end - 300000L, tz, 15) == 300000L, "five minutes left");
                check(FullBlockRules.remainingMillis(end - 1, tz, minutes) == 1, "one millisecond left");
                check(FullBlockRules.remainingMillis(end, tz, minutes) == 0, "hide at next hour");
                check(FullBlockRules.remainingMillis(start - 1, tz, minutes) == 0, "hide before window");
                check(FullBlockRules.windowActive(start + 3600000, tz, minutes), "repeats every hour");
            }
        }
        // Both repeated hours at a DST transition follow the displayed minute.
        for (String value : new String[]{"2026-11-01T01:50:00-04:00", "2026-11-01T01:50:00-05:00"})
            check(FullBlockRules.windowActive(java.time.OffsetDateTime.parse(value).toInstant().toEpochMilli(),
                    TimeZone.getTimeZone("America/New_York"), 10), "DST repeated hour");
        Set<String> exempt = new HashSet<>();
        check(FullBlockRules.countdownText(300000).equals("الحظر الشامل: 5 دقائق متبقية"), "requested label");
        check(FullBlockRules.countdownText(240001).equals("الحظر الشامل: 5 دقائق متبقية"), "round partial minute up");
        check(FullBlockRules.countdownText(240000).equals("الحظر الشامل: 4 دقائق متبقية"), "minute boundary");
        check(FullBlockRules.countdownText(1).contains("دقيقة واحدة"), "never show zero while active");
        check(FullBlockRules.countdownText(0).isEmpty(), "hide when inactive");
        check(FullBlockRules.countdownText(-1).isEmpty(), "hide after expiration");
        check(FullBlockRules.included(exempt, "app.a"), "default checked");
        exempt.add("app.a"); exempt.add("app.b"); exempt.add("app.c");
        check(!FullBlockRules.included(exempt, "app.a"), "unchecked app exempt");
        check(FullBlockRules.included(exempt, "app.new"), "new install checked");
        Set<String> draft = new HashSet<>(exempt);
        draft.remove("app.a");
        check(!FullBlockRules.included(exempt, "app.a"), "draft does not change saved selection");
        check(FullBlockRules.included(draft, "app.a"), "recheck removes exemption");
        System.out.println("PASS: hourly boundaries, local zones, DST, exemptions and live countdown rounding/expiry");
    }
}
