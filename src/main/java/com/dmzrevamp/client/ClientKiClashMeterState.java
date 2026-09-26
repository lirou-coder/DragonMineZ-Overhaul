package com.dmzrevamp.client;

public final class ClientKiClashMeterState {
    private static volatile float speedMultiplier = 1.0F;
    private static volatile float areaSizeMultiplier = 1.0F;
    private static volatile float perfectFraction = 0.35F;
    private static volatile float minimumGoodEfficiency = 0.5F;

    private ClientKiClashMeterState() {
    }

    public static void update(float speed, float area, float perfect, float minimumGood) {
        speedMultiplier = finitePositive(speed, 1.0F);
        areaSizeMultiplier = finitePositive(area, 1.0F);
        perfectFraction = finiteUnit(perfect, 0.35F);
        minimumGoodEfficiency = finiteUnit(minimumGood, 0.5F);
    }

    public static float speedMultiplier() { return speedMultiplier; }
    public static float areaSizeMultiplier() { return areaSizeMultiplier; }
    public static float perfectFraction() { return perfectFraction; }
    public static float minimumGoodEfficiency() { return minimumGoodEfficiency; }

    private static float finitePositive(float value, float fallback) {
        return Float.isFinite(value) && value > 0F ? value : fallback;
    }

    private static float finiteUnit(float value, float fallback) {
        return Float.isFinite(value) ? Math.max(0F, Math.min(1F, value)) : fallback;
    }
}
