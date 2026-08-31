package com.dmzrevamp.revamp.growth;

public final class DynamicGrowthAwardContext {
    private static final ThreadLocal<Double> TP_MULTIPLIER_OVERRIDE = new ThreadLocal<>();

    private DynamicGrowthAwardContext() {
    }

    public static void runWithTpMultiplier(double multiplier, Runnable action) {
        Double previous = TP_MULTIPLIER_OVERRIDE.get();
        TP_MULTIPLIER_OVERRIDE.set(Math.max(0D, multiplier));
        try {
            action.run();
        } finally {
            if (previous == null) {
                TP_MULTIPLIER_OVERRIDE.remove();
            } else {
                TP_MULTIPLIER_OVERRIDE.set(previous);
            }
        }
    }

    public static Double tpMultiplierOverride() {
        return TP_MULTIPLIER_OVERRIDE.get();
    }
}
