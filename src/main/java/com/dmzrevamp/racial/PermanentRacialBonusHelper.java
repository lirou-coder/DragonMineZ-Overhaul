// Shared helper for racial mechanics that grant permanent stat bonuses without letting them grow past configured caps.
package com.dmzrevamp.racial;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.GeneralServerConfig;
import com.dragonminez.common.stats.character.BonusStats;
import com.dragonminez.common.stats.StatsData;

import java.util.Arrays;
import java.util.Locale;

public final class PermanentRacialBonusHelper {
    // The helper only exposes stat math, so it should not be created as an object.
    private PermanentRacialBonusHelper() {
    }

    // Adds a permanent racial flat bonus while respecting the configured caps.
    public static boolean addOrAccumulateCappedStat(StatsData data,
                                                    String stat,
                                                    String bonusName,
                                                    double addedValue,
                                                    double capRatio,
                                                    String... sharedBonusNames) {
        if (data == null || stat == null || bonusName == null || addedValue <= 0D) {
            return false;
        }

        String normalizedStat = stat.toUpperCase(Locale.ROOT);
        if (!isCoreStat(normalizedStat)) {
            return false;
        }

        int rawStatValue = getRawStatValue(data, normalizedStat);
        BonusStats bonusStats = data.getBonusStats();
        double existingBonusValue = getNamedBonusValue(bonusStats, normalizedStat, bonusName);
        double sharedBonusValue = Arrays.stream(sharedBonusNames == null ? new String[0] : sharedBonusNames)
                .filter(name -> name != null && !name.equals(bonusName))
                .mapToDouble(name -> getNamedBonusValue(bonusStats, normalizedStat, name))
                .sum();

        int addedFlatValue = (int) Math.round(addedValue);
        if (addedFlatValue <= 0 && addedValue > 0D) {
            addedFlatValue = 1;
        }
        int availableRoom = getAvailableStatRoom(data, rawStatValue, existingBonusValue + sharedBonusValue);
        int statIncrease = Math.min(addedFlatValue, availableRoom);
        if (statIncrease <= 0 && existingBonusValue > 0D) {
            return existingBonusValue > 0D;
        }
        if (statIncrease <= 0) {
            return false;
        }

        bonusStats.removeBonus(normalizedStat, bonusName);
        bonusStats.addBonus(normalizedStat, bonusName, "+", existingBonusValue + statIncrease);
        return true;
    }

    public static boolean addOrAccumulateBaseCappedStat(StatsData data,
                                                        String stat,
                                                        String bonusName,
                                                        double addedValue,
                                                        double capRatio) {
        return addOrAccumulateBaseCappedStat(data, stat, bonusName, addedValue, capRatio, false);
    }

    public static boolean addOrAccumulateBaseCappedStat(StatsData data,
                                                        String stat,
                                                        String bonusName,
                                                        double addedValue,
                                                        double capRatio,
                                                        boolean applyMultipliers) {
        if (data == null || stat == null || bonusName == null || addedValue <= 0D || capRatio <= 0D) {
            return false;
        }

        String normalizedStat = stat.toUpperCase(Locale.ROOT);
        if (!isRacialBonusStat(normalizedStat)) {
            return false;
        }

        int baseStatValue = getBaseStatValueForRacialBonusCap(data, normalizedStat);
        if (baseStatValue <= 0) {
            return false;
        }

        BonusStats bonusStats = data.getBonusStats();
        double existingBonusValue = getNamedBonusValue(bonusStats, normalizedStat, bonusName);
        int availableRoom = Math.max(0, (int) Math.floor(baseStatValue * capRatio) - (int) Math.round(existingBonusValue));
        if (availableRoom <= 0) {
            return false;
        }

        int addedFlatValue = (int) Math.floor(addedValue);
        if (addedFlatValue <= 0 && addedValue > 0D) {
            addedFlatValue = 1;
        }

        int statIncrease = Math.min(addedFlatValue, availableRoom);
        if (statIncrease <= 0) {
            return false;
        }

        bonusStats.removeBonus(normalizedStat, bonusName);
        bonusStats.addBonus(normalizedStat, bonusName, "+", existingBonusValue + statIncrease, applyMultipliers);
        return true;
    }

    // Reads the player's raw core stat before Overhaul adds a new permanent racial bonus.
    public static int getRawStatValue(StatsData data, String statName) {
        String normalizedStat = statName.toUpperCase(Locale.ROOT);
        return switch (normalizedStat) {
            case "STR" -> data.getStats().getStrength();
            case "SKP" -> data.getStats().getStrikePower();
            case "RES" -> data.getStats().getResistance();
            case "VIT" -> data.getStats().getVitality();
            case "PWR" -> data.getStats().getKiPower();
            case "ENE" -> data.getStats().getEnergy();
            default -> 0;
        };
    }

    public static int getBaseStatValueForRacialBonusCap(StatsData data, String statName) {
        String normalizedStat = statName.toUpperCase(Locale.ROOT);
        return switch (normalizedStat) {
            case "DEF", "STM" -> data.getStats().getResistance();
            default -> getRawStatValue(data, normalizedStat);
        };
    }

    // Counts how much of one named bonus already exists on a stat, so new rewards can add to it instead of duplicating it.
    private static double getNamedBonusValue(BonusStats bonusStats, String stat, String bonusName) {
        return bonusStats.getBonuses(stat).stream()
                .filter(bonus -> bonusName.equals(bonus.name))
                .mapToDouble(bonus -> bonus.value)
                .sum();
    }

    // Accepts only DMZ's six base stats for older cap logic.
    private static boolean isCoreStat(String normalizedStat) {
        return switch (normalizedStat) {
            case "STR", "SKP", "RES", "VIT", "PWR", "ENE" -> true;
            default -> false;
        };
    }

    private static boolean isRacialBonusStat(String normalizedStat) {
        return switch (normalizedStat) {
            case "STR", "SKP", "RES", "DEF", "STM", "VIT", "PWR", "ENE" -> true;
            default -> false;
        };
    }

    // Calculates how many more flat points can be added before reaching DMZ's configured max stat value.
    private static int getAvailableStatRoom(StatsData data, int rawStatValue, double existingFlatBonusValue) {
        GeneralServerConfig serverConfig = ConfigManager.getServerConfig();
        Integer maxStatValue = serverConfig != null && serverConfig.getGameplay() != null
                ? serverConfig.getGameplay().getMaxValue()
                : null;
        if (maxStatValue == null || maxStatValue <= 0) {
            return Integer.MAX_VALUE;
        }
        return Math.max(0, maxStatValue - rawStatValue - (int) Math.round(existingFlatBonusValue));
    }
}
