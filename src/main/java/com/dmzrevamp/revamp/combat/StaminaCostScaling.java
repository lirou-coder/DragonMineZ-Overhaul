package com.dmzrevamp.revamp.combat;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.StatsData;

public final class StaminaCostScaling {
    private StaminaCostScaling() {
    }

    public static float applyTransformedStrengthDivisor(StatsData data, float cost) {
        if (cost <= 0.0F || data == null || data.getCharacter() == null) {
            return cost;
        }
        if (!data.getCharacter().hasActiveForm() && !data.getCharacter().hasActiveStackForm()) {
            return cost;
        }

        double divisor = getFormAndStackStrengthMultiplier(data);
        if (!Double.isFinite(divisor) || divisor <= 1.0D) {
            return cost;
        }
        return (float) Math.max(0.0D, cost / divisor);
    }

    private static double getFormAndStackStrengthMultiplier(StatsData data) {
        double formMultiplier = data.getFormMultiplier("STR");
        double stackMultiplier = data.getStackFormMultiplier("STR");
        if (ConfigManager.getServerConfig().getGameplay().getMultiplicationInsteadOfAdditionForMultipliers()) {
            return formMultiplier * stackMultiplier;
        }
        return 1.0D + (formMultiplier - 1.0D) + (stackMultiplier - 1.0D);
    }
}
