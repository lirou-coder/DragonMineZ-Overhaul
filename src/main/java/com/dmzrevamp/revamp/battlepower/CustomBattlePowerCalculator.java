package com.dmzrevamp.revamp.battlepower;

import com.dmzrevamp.config.CustomBattlePowerConfig;
import com.dragonminez.common.stats.StatsData;

public final class CustomBattlePowerCalculator {
    private static final double ANDROID_UPGRADED_BATTLE_POWER = 3.4028234663852886E38D;
    private static final double DEFAULT_SCOUTER_BREAK_BATTLE_POWER = 500000D;
    private static final double DEFAULT_REFERENCE_MULTIPLIER = 1200D;
    private static final double DEFAULT_TOTAL_STATS_DIVISOR = 1200D;
    private static final double DEFAULT_EXPONENT = 2.425D;
    private static final double MIN_NONZERO_PLAYER_BATTLE_POWER = 2D;
    private static final double SCOUTER_BREAK_REFERENCE_STATS = DEFAULT_TOTAL_STATS_DIVISOR
            * Math.pow(DEFAULT_SCOUTER_BREAK_BATTLE_POWER / DEFAULT_REFERENCE_MULTIPLIER, 1D / DEFAULT_EXPONENT);

    private CustomBattlePowerCalculator() {
    }

    public static double calculatePlayerBattlePower(StatsData data) {
        if (data == null) {
            return 0D;
        }
        if (data.getResources().getPowerRelease() <= 0) {
            return 0D;
        }
        if (data.getStatus().isAndroidUpgraded()) {
            return ANDROID_UPGRADED_BATTLE_POWER;
        }

        return enforcePlayerReleaseFloor(data, calculateFinitePlayerBattlePower(data));
    }

    public static double calculateFinitePlayerBattlePower(StatsData data) {
        if (data == null) {
            return 0D;
        }
        if (data.getResources().getPowerRelease() <= 0) {
            return 0D;
        }

        CustomBattlePowerConfig.Config config = CustomBattlePowerConfig.get();
        double totalStats = 0D;
        totalStats += CustomBattlePowerConfig.weightedValue(config.playerStats, "meleeDamage", data.getMaxMeleeDamage());
        totalStats += CustomBattlePowerConfig.weightedValue(config.playerStats, "strikeDamage", data.getMaxStrikeDamage());
        totalStats += CustomBattlePowerConfig.weightedValue(config.playerStats, "maxStamina", data.getMaxStamina());
        totalStats += CustomBattlePowerConfig.weightedValue(config.playerStats, "defense", data.getMaxFlatMitigation());
        totalStats += CustomBattlePowerConfig.weightedValue(config.playerStats, "maxHealth", data.getMaxHealth());
        totalStats += CustomBattlePowerConfig.weightedValue(config.playerStats, "kiDamage", data.getMaxKiDamage());
        totalStats += CustomBattlePowerConfig.weightedValue(config.playerStats, "maxKi", data.getMaxEnergy());

        double release = data.getResources().getPowerRelease() / 100D;
        return enforcePlayerReleaseFloor(data, calculate(config, totalStats, release));
    }

    /** Keeps the visible custom BP at zero only when the player has no release. */
    public static double enforcePlayerReleaseFloor(StatsData data, double battlePower) {
        if (data == null || data.getResources().getPowerRelease() <= 0) {
            return 0D;
        }
        if (!Double.isFinite(battlePower) || battlePower < MIN_NONZERO_PLAYER_BATTLE_POWER) {
            return MIN_NONZERO_PLAYER_BATTLE_POWER;
        }
        return battlePower;
    }

    public static double calculatePlayerBattlePowerFromValues(double meleeDamage,
                                                               double strikeDamage,
                                                               double maxStamina,
                                                               double defense,
                                                               double maxHealth,
                                                               double kiDamage,
                                                               double maxKi) {
        CustomBattlePowerConfig.Config config = CustomBattlePowerConfig.get();
        double totalStats = 0D;
        totalStats += CustomBattlePowerConfig.weightedValue(config.playerStats, "meleeDamage", meleeDamage);
        totalStats += CustomBattlePowerConfig.weightedValue(config.playerStats, "strikeDamage", strikeDamage);
        totalStats += CustomBattlePowerConfig.weightedValue(config.playerStats, "maxStamina", maxStamina);
        totalStats += CustomBattlePowerConfig.weightedValue(config.playerStats, "defense", defense);
        totalStats += CustomBattlePowerConfig.weightedValue(config.playerStats, "maxHealth", maxHealth);
        totalStats += CustomBattlePowerConfig.weightedValue(config.playerStats, "kiDamage", kiDamage);
        totalStats += CustomBattlePowerConfig.weightedValue(config.playerStats, "maxKi", maxKi);
        double battlePower = calculate(config, totalStats, 1D);
        return Double.isFinite(battlePower) && battlePower >= MIN_NONZERO_PLAYER_BATTLE_POWER
                ? battlePower : MIN_NONZERO_PLAYER_BATTLE_POWER;
    }

    public static long calculateMobBattlePower(double totalStats) {
        double calculated = calculateMobBattlePowerExact(totalStats);
        if (calculated >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return Math.max(1L, (long) calculated);
    }

    public static double calculateMobBattlePowerExact(double totalStats) {
        return Math.max(1D, calculate(CustomBattlePowerConfig.get(), totalStats, 1D));
    }

    /** Inverse of the configured mob BP curve, used when an addon supplies a target BP. */
    public static double totalStatsForMobBattlePower(double battlePower) {
        CustomBattlePowerConfig.Config config = CustomBattlePowerConfig.get();
        if (!Double.isFinite(battlePower) || battlePower <= 0D
                || !Double.isFinite(config.referenceMultiplier) || config.referenceMultiplier <= 0D
                || !Double.isFinite(config.totalStatsDivisor) || config.totalStatsDivisor <= 0D
                || !Double.isFinite(config.exponent) || config.exponent <= 0D) {
            return 0D;
        }
        double result = config.totalStatsDivisor
                * Math.pow(battlePower / config.referenceMultiplier, 1D / config.exponent);
        return Double.isFinite(result) && result > 0D ? result : 0D;
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
