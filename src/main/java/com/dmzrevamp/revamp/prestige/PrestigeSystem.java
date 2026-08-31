package com.dmzrevamp.revamp.prestige;

import com.dmzrevamp.config.LevelingRevampConfig;
import com.dmzrevamp.revamp.DmzRevampHelper;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.config.ConfigManager;

public final class PrestigeSystem {
    public static final String FINAL_PRESTIGE_QUEST = "prestige_saga:3";

    private PrestigeSystem() {
    }

    public static int count(StatsData data) {
        return data instanceof PrestigeDataAccess access ? access.dmzrevamp$getPrestigeCount() : 0;
    }

    public static void setCount(StatsData data, int count) {
        if (data instanceof PrestigeDataAccess access) access.dmzrevamp$setPrestigeCount(count);
    }

    public static int levelCap(StatsData data) {
        var config = LevelingRevampConfig.get();
        if (!config.levelsAndAttributes.enabled) return ConfigManager.getServerConfig().getGameplay().getMaxValue();
        if (!config.Prestige.enabled) return config.levelsAndAttributes.maxLevel;
        int prestige = Math.min(count(data), config.Prestige.maxPrestigeCount);
        if (prestige >= config.Prestige.maxPrestigeCount) return config.levelsAndAttributes.maxLevel;
        double step = (config.Prestige.initialLevelCap / (double) config.Prestige.maxPrestigeCount)
                + ((config.levelsAndAttributes.maxLevel - config.Prestige.initialLevelCap)
                / (double) config.Prestige.maxPrestigeCount);
        return (int) Math.min(config.levelsAndAttributes.maxLevel,
                Math.floor(config.Prestige.initialLevelCap + prestige * step));
    }

    public static int maximumAttribute() {
        var levels = LevelingRevampConfig.get().levelsAndAttributes;
        if (levels.maxAttribute >= 0) return levels.maxAttribute;
        long unboundedByAttribute = (long) levels.maxLevel * levels.levelUpPerPoints;
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1L, unboundedByAttribute));
    }

    public static int attributeFormulaMaximum() {
        var levels = LevelingRevampConfig.get().levelsAndAttributes;
        if (levels.maxAttribute >= 0) return Math.max(1, levels.maxAttribute);
        long levelPointMaximum = (long) levels.maxLevel * levels.levelUpPerPoints;
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1L, levelPointMaximum));
    }

    /**
     * Returns the reference used by the statistics hexagon. Unlike the combat
     * formula reference, this may follow the player's current prestige cap so
     * the graph remains useful throughout progression.
     */
    public static double hexStatReference(StatsData data) {
        var config = LevelingRevampConfig.get();
        var levels = config.levelsAndAttributes;
        if (!config.Prestige.prestigeHexStatChange) {
            return attributeFormulaMaximum();
        }

        double currentLevelCap = Math.max(1D, levelCap(data));
        if (levels.maxAttribute < 0) {
            return Math.max(1D, currentLevelCap * levels.levelUpPerPoints);
        }
        double attributePerLevel = levels.maxAttribute / (double) Math.max(1, levels.maxLevel);
        return Math.max(1D, currentLevelCap * attributePerLevel);
    }

    /**
     * Returns the per-player attribute ceiling used while assigning stats.
     * The Forge attribute's global numeric maximum remains unchanged; only the
     * player's assignable amount follows their current prestige level cap.
     */
    public static int effectiveMaximumAttribute(StatsData data) {
        var config = LevelingRevampConfig.get();
        var levels = config.levelsAndAttributes;
        if (levels.maxAttribute < 0) return Integer.MAX_VALUE;
        if (!config.Prestige.usePrestigeMaxAttribute) return levels.maxAttribute;

        double proportionalMaximum = levelCap(data)
                * (levels.maxAttribute / (double) Math.max(1, levels.maxLevel));
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1D, Math.floor(proportionalMaximum)));
    }

    public static int movementFormulaMaximum() {
        var levels = LevelingRevampConfig.get().levelsAndAttributes;
        return Math.max(1, levels.maxLevel);
    }

    public static int maxAssignableTotal(StatsData data) {
        long initial = DmzRevampHelper.getInitialStatTotal(data);
        long earned = (long) Math.max(0, levelCap(data) - 1)
                * LevelingRevampConfig.get().levelsAndAttributes.levelUpPerPoints;
        return (int) Math.min(Integer.MAX_VALUE, initial + earned);
    }

    public static boolean canPrestige(StatsData data) {
        var config = LevelingRevampConfig.get();
        return LevelingRevampConfig.prestigeEnabled()
                && count(data) < config.Prestige.maxPrestigeCount
                && data.getLevel() >= levelCap(data)
                && data.getPlayerQuestData().isQuestCompleted(FINAL_PRESTIGE_QUEST);
    }

    public static double scaleMultiplier(StatsData data) {
        if (!LevelingRevampConfig.prestigeEnabled()) return 1D;
        return 1D + count(data) * LevelingRevampConfig.get().Prestige.scaleBonusPerPrestige;
    }

    public static double tpMultiplier(StatsData data) {
        if (!LevelingRevampConfig.prestigeEnabled()) return 1D;
        return 1D + count(data) * LevelingRevampConfig.get().Prestige.TPBonusPerPrestige;
    }

    public static double masteryMultiplier(StatsData data) {
        if (!LevelingRevampConfig.prestigeEnabled()) return 1D;
        return 1D + count(data) * LevelingRevampConfig.get().Prestige.masteryBonusPerPrestige;
    }

    public static double storyRewardMultiplier(StatsData data) {
        if (!LevelingRevampConfig.prestigeEnabled()) return 1D;
        return 1D + count(data) * LevelingRevampConfig.get().Prestige.storyRewardBonusPerPrestige;
    }

    public static double storyDifficultyMultiplier(StatsData data) {
        if (!LevelingRevampConfig.prestigeEnabled()) return 1D;
        return 1D + count(data) * LevelingRevampConfig.get().Prestige.storyDifficultyIncreasePerPrestige;
    }

    public static double roundedDifficultyValue(double value) {
        if (!Double.isFinite(value)) return 1D;
        return Math.round(value * 100D) / 100D;
    }
}
