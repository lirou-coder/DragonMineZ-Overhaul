package com.dmzrevamp.revamp.battlepower;

import com.dmzrevamp.config.CustomBattlePowerConfig;
import com.dragonminez.common.stats.StatsData;

public final class CustomBattlePowerCalculator {
    private static final double ANDROID_UPGRADED_BATTLE_POWER = 3.4028234663852886E38D;
    private static final double DEFAULT_SCOUTER_BREAK_BATTLE_POWER = 150000D;
    private static final double DEFAULT_REFERENCE_MULTIPLIER = 1200D;
    private static final double DEFAULT_TOTAL_STATS_DIVISOR = 500D;
    private static final double DEFAULT_EXPONENT = 1.2D;
    private static final double SCOUTER_BREAK_REFERENCE_STATS = DEFAULT_TOTAL_STATS_DIVISOR
            * Math.pow(DEFAULT_SCOUTER_BREAK_BATTLE_POWER / DEFAULT_REFERENCE_MULTIPLIER, 1D / DEFAULT_EXPONENT);

    private CustomBattlePowerCalculator() {
    }

    public static double calculatePlayerBattlePower(StatsData data) {
        if (data == null) {
            return 0D;
        }
        if (data.getStatus().isAndroidUpgraded()) {
            return ANDROID_UPGRADED_BATTLE_POWER;
        }

        CustomBattlePowerConfig.Config config = CustomBattlePowerConfig.get();
        double totalStats = 0D;
        totalStats += CustomBattlePowerConfig.weightedValue(config.playerStats, "meleeDamage", data.getMaxMeleeDamage());
        totalStats += CustomBattlePowerConfig.weightedValue(config.playerStats, "strikeDamage", data.getMaxStrikeDamage());
        totalStats += CustomBattlePowerConfig.weightedValue(config.playerStats, "maxStamina", data.getMaxStamina());
        totalStats += CustomBattlePowerConfig.weightedValue(config.playerStats, "defense", data.getMaxDefense());
        totalStats += CustomBattlePowerConfig.weightedValue(config.playerStats, "maxHealth", data.getMaxHealth());
        totalStats += CustomBattlePowerConfig.weightedValue(config.playerStats, "kiDamage", data.getMaxKiDamage());
        totalStats += CustomBattlePowerConfig.weightedValue(config.playerStats, "maxKi", data.getMaxEnergy());

        double release = data.getResources().getPowerRelease() / 100D;
        return calculate(config, totalStats, release);
    }

    public static long calculateMobBattlePower(double totalStats) {
        double calculated = calculate(CustomBattlePowerConfig.get(), totalStats, 1D);
        if (calculated >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return Math.max(1L, (long) calculated);
    }

    public static double calculateScouterBreakBattlePower() {
        return calculate(CustomBattlePowerConfig.get(), SCOUTER_BREAK_REFERENCE_STATS, 1D);
    }

    private static double calculate(CustomBattlePowerConfig.Config config, double totalStats, double release) {
        if (!Double.isFinite(totalStats) || totalStats <= 0D || !Double.isFinite(release) || release <= 0D) {
            return 0D;
        }
        double curved = config.referenceMultiplier * Math.pow(totalStats / config.totalStatsDivisor, config.exponent) * release;
        if (!Double.isFinite(curved) || curved <= 0D) {
            return 0D;
        }
        return curved;
    }
}
