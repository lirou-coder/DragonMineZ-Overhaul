package com.dmzrevamp.client;

/**
 * Shared danger styling used by both Ki Sense combat labels and search auras.
 * Thresholds deliberately resolve exactly 120% to orange; the light-red tier
 * starts above 120% and ends at 180%.
 */
public final class KiSenseDangerStyle {
    public static final int LIGHT_BLUE = 0x55FFFF;
    public static final int YELLOW = 0xFFFF55;
    public static final int ORANGE = 0xFFAA00;
    public static final int DARK_ORANGE = 0xFF3B00;
    public static final int RED = 0xBA0003;

    private KiSenseDangerStyle() {
    }

    public static Tier tier(double ratio) {
        if (!Double.isFinite(ratio) || ratio < 0D || ratio < 0.85D) {
            return Tier.LIGHT_BLUE;
        }
        if (ratio < 1.16D) {
            return Tier.YELLOW;
        }
        if (ratio <= 1.20D) {
            return Tier.ORANGE;
        }
        if (ratio < 1.81D) {
            return Tier.DARK_ORANGE;
        }
        return Tier.RED;
    }

    public static int color(double ratio) {
        return switch (tier(ratio)) {
            case LIGHT_BLUE -> LIGHT_BLUE;
            case YELLOW -> YELLOW;
            case ORANGE -> ORANGE;
            case DARK_ORANGE -> DARK_ORANGE;
            case RED -> RED;
        };
    }

    /**
     * Scale applied to the normal 0.6 Ki Sense combat BP label size.
     * The final red tier intentionally remains at the light-red tier's size.
     */
    public static float combatLabelScale(double ratio) {
        return switch (tier(ratio)) {
            case LIGHT_BLUE -> 1.0F;
            case YELLOW -> 1.2F;
            case ORANGE -> 1.4F;
            case DARK_ORANGE, RED -> 1.75F;
        };
    }

    /**
     * Replacement for DMZ's search-aura powerMul. DMZ normally caps that
     * multiplier at 3.0; the 181%+ tier is explicitly twice that maximum.
     */
    public static double searchAuraScale(double ratio) {
        return switch (tier(ratio)) {
            case LIGHT_BLUE -> 1.0D;
            case YELLOW -> 1.5D;
            case ORANGE -> 2.0D;
            case DARK_ORANGE -> 3.0D;
            case RED -> 5.0D;
        };
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

    public enum Tier {
        LIGHT_BLUE,
        YELLOW,
        ORANGE,
        DARK_ORANGE,
        RED
    }
}
