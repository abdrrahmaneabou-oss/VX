package com.v1f.app.patch;

import java.lang.reflect.Method;

/**
 * Reads the existing V2F runtime policy without compile-time coupling to the
 * base APK classes. Editing is allowed only when the existing runtime says
 * all apps are allowed (inactive protection or an open break).
 */
public final class StrictBlockPolicy {
    private StrictBlockPolicy() {}

    public static boolean allowsAll() {
        try {
            Class<?> runtime = Class.forName("com.v1f.app.patch.BreakRuntime");
            Method method = runtime.getDeclaredMethod("allowsAll");
            method.setAccessible(true);
            Object result = method.invoke(null);
            return Boolean.TRUE.equals(result);
        } catch (Throwable ignored) {
            // Fail closed: if the protection state cannot be read, never allow edits.
            return false;
        }
    }
}
