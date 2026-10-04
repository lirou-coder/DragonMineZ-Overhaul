package com.dmzrevamp.util;

import java.util.Locale;

/** Formats large stat values without narrowing them to Minecraft's legacy integer BP range. */
public final class CompactNumberFormatter {
    private static final String[] SUFFIXES = {
            "", "K", "M", "B", "T", "Qa", "Qi", "Sx", "Sp", "Oc", "No", "Dc", "Ud", "Dd"
    };

    private CompactNumberFormatter() {
    }

    public static String format(double value, double abbreviationThreshold) {
        if (!Double.isFinite(value)) return "0";
        double absolute = Math.abs(value);
        if (absolute < abbreviationThreshold) {
            return String.format(Locale.ROOT, "%,.0f", value);
        }

        int group = 0;
        double scaled = value;
        while (Math.abs(scaled) >= 1_000D && group < SUFFIXES.length - 1) {
            scaled /= 1_000D;
            group++;
        }
        if (group == SUFFIXES.length - 1 && Math.abs(scaled) >= 1_000D) {
            return String.format(Locale.ROOT, "%.2e", value);
        }
        return trim(scaled) + SUFFIXES[group];
    }

    private static String trim(double value) {
        String text = String.format(Locale.ROOT, "%.2f", value);
        while (text.endsWith("0")) text = text.substring(0, text.length() - 1);
        return text.endsWith(".") ? text.substring(0, text.length() - 1) : text;
    }
}
