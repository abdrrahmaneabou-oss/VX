package com.v1f.app.patch;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;

/** Settings additions only run during a user-started, unexpired protection session. */
public final class SettingsSessionGate {
    private SettingsSessionGate() {}

    public static boolean isSessionRunning() {
        try {
            Class<?> graphClass = Class.forName("com.v1f.app.AppGraph");
            Object graph = graphClass.getField("INSTANCE").get(null);
            Object repo = graphClass.getMethod("getSessionRepository").invoke(graph);
            Object flow = repo.getClass().getMethod("getSession").invoke(repo);
            Object session = flow.getClass().getMethod("getValue").invoke(flow);
            if (session == null) return false;
            Method remainingMethod = repo.getClass().getMethod("remainingMillis");
            Object remaining = remainingMethod.invoke(repo);
            return remaining instanceof Number && ((Number) remaining).longValue() > 0L;
        } catch (Throwable ignored) {
            return false; // Never enable Settings interception without a verified session.
        }
    }

    /** A break suspends every Settings guard, even while the session clock runs. */
    public static boolean isActive() {
        return BreakPauseRules.protectionEnabled(isSessionRunning(), StrictBlockPolicy.allowsAll());
    }

    public static Set<String> blockedLabelsSnapshot() {
        return isActive() ? DynamicLabelsStore.snapshot() : Collections.<String>emptySet();
    }
}
