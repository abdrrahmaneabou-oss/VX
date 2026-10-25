package com.v1f.app.patch;

import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A defensive detector for Settings rows whose click event does not expose the
 * visible label directly. The stock V2F detector only inspects the click event
 * and its source; several OEM Settings implementations report a child/chevron
 * instead of the visible row label.
 */
public final class RobustSettingsGuard {
    private static final String SETTINGS_PACKAGE = "com.android.settings";
    private static final String RESET_ROW = "Reset app preferences";
    private static final String RESET_DIALOG = "Reset app preferences?";
    private static final String RESET_BUTTON = "RESET APPS";
    private static final int MAX_DIALOG_NODES = 96;
    private static final int MAX_DIALOG_DEPTH = 7;

    private RobustSettingsGuard() {}

    public static String detect(AccessibilityEvent event, String packageName) {
        if (event == null || !SETTINGS_PACKAGE.equals(packageName)) return null;

        Set<String> blocked = DynamicLabelsStore.snapshot();
        if (blocked == null || blocked.isEmpty()) return null;

        final int type = event.getEventType();
        if (type == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            return detectClick(event, blocked);
        }

        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                || type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                || type == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                String openedPage = detectKnownProtectedPage(event, blocked);
                if (openedPage != null) return openedPage;
            }
            return detectDialog(event, blocked);
        }

        return null;
    }

    private static String detectClick(AccessibilityEvent event, Set<String> blocked) {
        LinkedHashSet<String> texts = new LinkedHashSet<>();
        addEventText(event, texts);

        AccessibilityNodeInfo source = safeSource(event);
        collectNode(source, texts, 3, new int[] { 32 });

        String direct = firstBlocked(texts, blocked);
        if (direct != null) return direct;

        // REDMAGIC sometimes reports the icon or a nested child of the row.
        // Follow up to four parent levels but stop at the FIRST clickable row,
        // never scanning an entire Settings list for unrelated blocked labels.
        AccessibilityNodeInfo parent = safeParent(source);
        for (int level = 0; parent != null && level < 4; level++) {
            try { add(texts, parent.getText()); } catch (RuntimeException ignored) {}
            try { add(texts, parent.getContentDescription()); } catch (RuntimeException ignored) {}
            String fromParent = firstBlocked(texts, blocked);
            if (fromParent != null) return fromParent;
            boolean isRow = false;
            try { isRow = parent.isClickable(); } catch (RuntimeException ignored) {}
            if (isRow) {
                collectNode(parent, texts, 3, new int[] { 48 });
                return firstBlocked(texts, blocked);
            }
            parent = safeParent(parent);
        }

        return null;
    }

    /**
     * A second opportunity when a click was missed: the actual Settings page
     * became active. Match a known Activity identity, NOT text from the home
     * menu (which contains labels for pages not currently open).
     */
    private static String detectKnownProtectedPage(AccessibilityEvent event,
                                                    Set<String> blocked) {
        CharSequence className = event.getClassName();
        if (className == null) return null;
        String activity = className.toString();
        if (activity.endsWith("Settings$SecurityDashboardActivity")
                && containsNormalized(blocked, "Security & privacy")) {
            return "Security & privacy";
        }
        if (activity.endsWith("Settings$MoreSecurityPrivacySettingsActivity")
                && containsNormalized(blocked, "More security & privacy")) {
            return "More security & privacy";
        }
        return null;
    }

    private static String detectDialog(AccessibilityEvent event, Set<String> blocked) {
        LinkedHashSet<String> texts = new LinkedHashSet<>();
        addEventText(event, texts);

        AccessibilityNodeInfo source = safeSource(event);
        collectNode(source, texts, MAX_DIALOG_DEPTH, new int[] { MAX_DIALOG_NODES });

        // A window/content event may point at one child of a dialog. Walk a few
        // parents so the title and action button are still observable.
        AccessibilityNodeInfo cursor = source;
        for (int i = 0; i < 4 && cursor != null; i++) {
            cursor = safeParent(cursor);
            collectNode(cursor, texts, 3, new int[] { 48 });
        }

        boolean resetRequested = containsNormalized(blocked, RESET_ROW)
                || containsNormalized(blocked, RESET_DIALOG);
        if (resetRequested
                && (containsNormalized(texts, RESET_DIALOG)
                    || containsNormalized(texts, RESET_BUTTON))) {
            return RESET_DIALOG;
        }

        // Permit other explicitly configured dialog titles while avoiding a
        // broad root-window scan for ordinary row labels.
        for (String label : blocked) {
            String normalized = normalize(label);
            if (normalized != null && normalized.endsWith("?")
                    && containsNormalized(texts, normalized)) {
                return normalized;
            }
        }
        return null;
    }

    private static void addEventText(AccessibilityEvent event, Set<String> out) {
        try {
            Collection<CharSequence> values = event.getText();
            if (values != null) {
                for (CharSequence value : values) add(out, value);
            }
        } catch (Throwable ignored) {}
        try {
            add(out, event.getContentDescription());
        } catch (Throwable ignored) {}
    }

    private static AccessibilityNodeInfo safeSource(AccessibilityEvent event) {
        try {
            return event.getSource();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static AccessibilityNodeInfo safeParent(AccessibilityNodeInfo node) {
        if (node == null) return null;
        try {
            return node.getParent();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void collectNode(
            AccessibilityNodeInfo node,
            Set<String> out,
            int depth,
            int[] budget
    ) {
        if (node == null || depth < 0 || budget[0] <= 0) return;
        budget[0]--;

        try { add(out, node.getText()); } catch (Throwable ignored) {}
        try { add(out, node.getContentDescription()); } catch (Throwable ignored) {}

        if (depth == 0) return;
        int count;
        try {
            count = node.getChildCount();
        } catch (Throwable ignored) {
            return;
        }
        for (int i = 0; i < count && budget[0] > 0; i++) {
            AccessibilityNodeInfo child;
            try {
                child = node.getChild(i);
            } catch (Throwable ignored) {
                child = null;
            }
            collectNode(child, out, depth - 1, budget);
        }
    }

    private static void add(Set<String> out, CharSequence value) {
        if (value == null) return;
        String normalized = normalize(value.toString());
        if (normalized != null) out.add(normalized);
    }

    private static String firstBlocked(Iterable<String> candidates, Set<String> blocked) {
        for (String candidate : candidates) {
            String normalizedCandidate = normalize(candidate);
            if (normalizedCandidate == null) continue;
            if (containsNormalized(blocked, normalizedCandidate)) return normalizedCandidate;
        }
        return null;
    }

    private static boolean containsNormalized(Iterable<String> values, String wanted) {
        String normalizedWanted = normalize(wanted);
        if (normalizedWanted == null) return false;
        for (String value : values) {
            String normalized = normalize(value);
            if (normalizedWanted.equals(normalized)) return true;
        }
        return false;
    }

    private static String normalize(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
