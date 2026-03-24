package com.dailyserviceapp.core.utils;

import android.graphics.Color;

import java.util.Locale;

/**
 * Utility methods for profile avatar display in drawer/profile header areas.
 */
public final class AvatarUtils {

    private static final int[] AVATAR_COLORS = new int[] {
        Color.parseColor("#1565C0"),
        Color.parseColor("#2E7D32"),
        Color.parseColor("#6A1B9A"),
        Color.parseColor("#EF6C00"),
        Color.parseColor("#00838F"),
        Color.parseColor("#C62828"),
        Color.parseColor("#4527A0"),
        Color.parseColor("#00695C")
    };

    private AvatarUtils() {
        // Utility class
    }

    public static String resolveDisplayName(String preferredName, String fallbackEmail, String defaultName) {
        String trimmedName = safeTrim(preferredName);
        if (!trimmedName.isEmpty()) {
            return trimmedName;
        }

        String trimmedEmail = safeTrim(fallbackEmail);
        if (!trimmedEmail.isEmpty()) {
            int atIndex = trimmedEmail.indexOf('@');
            if (atIndex > 0) {
                return trimmedEmail.substring(0, atIndex);
            }
            return trimmedEmail;
        }

        return safeTrim(defaultName).isEmpty() ? "User" : defaultName;
    }

    public static String getInitials(String rawName) {
        String name = safeTrim(rawName);
        if (name.isEmpty()) {
            return "U";
        }

        String[] parts = name.split("\\s+");
        if (parts.length == 1) {
            String first = parts[0];
            if (first.length() >= 2) {
                return first.substring(0, 2).toUpperCase(Locale.getDefault());
            }
            return first.substring(0, 1).toUpperCase(Locale.getDefault());
        }

        String firstInitial = parts[0].substring(0, 1);
        String lastPart = parts[parts.length - 1];
        String lastInitial = lastPart.isEmpty() ? "" : lastPart.substring(0, 1);
        return (firstInitial + lastInitial).toUpperCase(Locale.getDefault());
    }

    public static int getAvatarColor(String seed) {
        String value = safeTrim(seed);
        if (value.isEmpty()) {
            return AVATAR_COLORS[0];
        }
        int index = Math.abs(value.toLowerCase(Locale.US).hashCode()) % AVATAR_COLORS.length;
        return AVATAR_COLORS[index];
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}