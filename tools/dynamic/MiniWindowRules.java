package com.v1f.app.patch;

/** Conservative bounds check for OEM floating / Mini application windows. */
public final class MiniWindowRules {
    private MiniWindowRules() {}

    public static boolean isMiniBounds(int width, int height, int screenWidth, int screenHeight) {
        if (screenWidth <= 0 || screenHeight <= 0
                || width <= 0 || height <= 0 || width > screenWidth || height > screenHeight)
            return false;
        long area = (long) width * height;
        long screen = (long) screenWidth * screenHeight;
        return width > screenWidth / 5 && height > screenHeight / 8
                && area > screen / 12
                && (width < screenWidth * 0.90f || height < screenHeight * 0.75f);
    }
}
