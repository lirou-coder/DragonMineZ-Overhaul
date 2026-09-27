package com.dmzrevamp.client;

/** Shared continuous danger styling used by Ki Sense combat labels and search auras. */
public final class KiSenseDangerStyle {
    public static final int LIGHT_BLUE = 0x55FFFF;
    public static final int YELLOW = 0xFFFF55;
    public static final int ORANGE = 0xFFAA00;
    public static final int DARK_ORANGE = 0xFF3B00;
    public static final int RED = 0xBA0003;
    public static final int PURPLE = 0xAA00AA;

    private static final double[] RATIOS = {0D, 0.85D, 1.16D, 1.20D, 1.81D, 5.0D};
    private static final int[] COLORS = {LIGHT_BLUE, YELLOW, ORANGE, DARK_ORANGE, RED, PURPLE};
    private static final float[] COMBAT_LABEL_SCALES = {1.0F, 1.2F, 1.4F, 1.75F, 2.0F, 2.5F};
    private static final double[] SEARCH_AURA_SCALES = {1.0D, 1.5D, 2.0D, 2.5D, 4.0D, 6.0D};

    private KiSenseDangerStyle() {
    }

    public static int color(double ratio) {
        Segment segment = segment(ratio);
        return lerpColor(COLORS[segment.lowerIndex], COLORS[segment.upperIndex], segment.progress);
    }

    /** Scale multiplier applied to the normal 0.6 Ki Sense combat BP label. */
    public static float combatLabelScale(double ratio) {
        Segment segment = segment(ratio);
        return (float) lerp(COMBAT_LABEL_SCALES[segment.lowerIndex],
                COMBAT_LABEL_SCALES[segment.upperIndex], segment.progress);
    }

    /** Continuous replacement for DMZ's search-aura power multiplier. */
    public static double searchAuraScale(double ratio) {
        Segment segment = segment(ratio);
        return lerp(SEARCH_AURA_SCALES[segment.lowerIndex],
                SEARCH_AURA_SCALES[segment.upperIndex], segment.progress);
    }

    public static float[][] auraColors(double ratio) {
        int color = color(ratio);
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        return new float[][]{{r, g, b}, {r, g, b}};
    }

    public static double ratio(float ownBattlePower, float targetBattlePower) {
        if (!Float.isFinite(ownBattlePower) || ownBattlePower <= 0F
                || !Float.isFinite(targetBattlePower) || targetBattlePower < 0F) {
            return 0D;
        }
        return targetBattlePower / (double) ownBattlePower;
    }

    private static Segment segment(double ratio) {
        double safeRatio = Double.isFinite(ratio) ? Math.max(0D, ratio) : 0D;
        if (safeRatio >= RATIOS[RATIOS.length - 1]) {
            int last = RATIOS.length - 1;
            return new Segment(last, last, 0D);
        }
        for (int upper = 1; upper < RATIOS.length; upper++) {
            if (safeRatio <= RATIOS[upper]) {
                int lower = upper - 1;
                double width = RATIOS[upper] - RATIOS[lower];
                double progress = width <= 0D ? 0D : (safeRatio - RATIOS[lower]) / width;
                return new Segment(lower, upper, Math.max(0D, Math.min(1D, progress)));
            }
        }
        int last = RATIOS.length - 1;
        return new Segment(last, last, 0D);
    }

    private static int lerpColor(int from, int to, double progress) {
        int red = (int) Math.round(lerp((from >> 16) & 0xFF, (to >> 16) & 0xFF, progress));
        int green = (int) Math.round(lerp((from >> 8) & 0xFF, (to >> 8) & 0xFF, progress));
        int blue = (int) Math.round(lerp(from & 0xFF, to & 0xFF, progress));
        return (red << 16) | (green << 8) | blue;
    }

    private static double lerp(double from, double to, double progress) {
        return from + (to - from) * progress;
    }

    private record Segment(int lowerIndex, int upperIndex, double progress) {}
}
