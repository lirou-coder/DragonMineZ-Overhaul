package com.dmzrevamp.revamp;

import com.dmzrevamp.config.DmzRevampConfig;
import com.dmzrevamp.config.LevelingRevampConfig;
import com.dmzrevamp.config.WeightMovementPenaltyConfig;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.config.GeneralServerConfig;
import com.dragonminez.common.config.RaceStatsConfig;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.extras.FormMasteries;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.server.util.GravityLogic;
import net.minecraft.world.entity.player.Player;

public final class DmzRevampHelper {
    private static final int TOTAL_CORE_STATS = 6;

    // This class centralizes Overhaul stat math and should not be created as an object.
    private DmzRevampHelper() {
    }

    // Keeps old configs that still say "3 points per level" compatible with Overhaul's six-stat level math.
    public static int getConfiguredStatPointsPerLevel() {
        int configured = LevelingRevampConfig.levelsEnabled()
                ? LevelingRevampConfig.get().levelsAndAttributes.levelUpPerPoints
                : 6;
        return Math.max(1, configured == 3 ? 6 : configured);
    }

    // Adds the race/class starting stats so level calculations only count points earned after character creation.
    public static int getInitialStatTotal(StatsData data) {
        Character character = data.getCharacter();
        RaceStatsConfig raceConfig = ConfigManager.getRaceStats(character.getRaceName());
        RaceStatsConfig.ClassStats classStats = raceConfig != null ? raceConfig.getClassStats(character.getCharacterClass()) : null;
        RaceStatsConfig.BaseStats baseStats = classStats != null ? classStats.getBaseStats() : null;
        if (baseStats == null) {
            RaceStatsConfig defaultConfig = new RaceStatsConfig();
            RaceStatsConfig.ClassStats defaultClassStats = defaultConfig.getClassStats(character.getCharacterClass());
            baseStats = defaultClassStats != null ? defaultClassStats.getBaseStats() : null;
        }
        if (baseStats == null) {
            return 0;
        }

        return getInt(baseStats.getStrength())
                + getInt(baseStats.getStrikePower())
                + getInt(baseStats.getResistance())
                + getInt(baseStats.getVitality())
                + getInt(baseStats.getKiPower())
                + getInt(baseStats.getEnergy());
    }

    // Rebuilds the displayed level from distributed stat points instead of trusting DMZ's original three-stat formula.
    public static int getConfiguredLevel(StatsData data) {
        int totalStats = data.getStats().getTotalStats();
        int baseStats = getInitialStatTotal(data);
        int distributedPoints = Math.max(0, totalStats - baseStats);
        int level = (distributedPoints / getConfiguredStatPointsPerLevel()) + 1;
        return LevelingRevampConfig.levelsEnabled()
                ? Math.min(com.dmzrevamp.revamp.prestige.PrestigeSystem.levelCap(data), Math.max(1, level))
                : Math.max(1, level);
    }

    // Calculates how many stat points a TP spend can buy without exceeding TP balance or the configured stat cap.
    public static int calculateStatIncrease(StatsData data, int maxStatsToAdd, int availableTPs, int maxStats) {
        int statsIncreased = 0;
        int costAccumulated = 0;
        int currentTotalStats = data.getStats().getTotalStats();
        int effectiveCap = clampToInt((long) maxStats * TOTAL_CORE_STATS);

        while (statsIncreased < maxStatsToAdd) {
            if (currentTotalStats + statsIncreased >= effectiveCap) {
                break;
            }

            int costForNext = data.getSingleStatCost(currentTotalStats + statsIncreased);
            if (costAccumulated + costForNext > availableTPs) {
                break;
            }

            costAccumulated += costForNext;
            statsIncreased++;
        }

        return statsIncreased;
    }

    // Returns a stat after bonuses, forms, scaling, and the player's current power release percentage.
    public static double getCurrentStatFormula(StatsData data, String statName) {
        return getBaseStatFormula(data, statName) * (data.getResources().getPowerRelease() / 100D);
    }

    // Returns a stat after bonuses, forms, and scaling, before power release lowers the active value.
    public static double getBaseStatFormula(StatsData data, String statName) {
        String normalizedStatName = statName.toUpperCase();
        double statValue = switch (normalizedStatName) {
            case "STR" -> data.getStats().getStrength();
            case "SKP" -> data.getStats().getStrikePower();
            case "RES" -> data.getStats().getResistance();
            case "VIT" -> data.getStats().getVitality();
            case "PWR" -> data.getStats().getKiPower();
            case "ENE" -> data.getStats().getEnergy();
            default -> 0D;
        };
        double statBonus = data.getBonusStats().calculateBonus(normalizedStatName, (int) Math.round(statValue), false);
        double multipliedStatBonus = data.getBonusStats().calculateBonus(normalizedStatName, (int) Math.round(statValue), true);
        return ((statValue + multipliedStatBonus)
                * data.getStatScaling(normalizedStatName)
                * data.getTotalMultiplier(normalizedStatName))
                + (statBonus * data.getStatScaling(normalizedStatName));
    }

    // Converts active SPD into the movement percentage shown in the stats screen.
    public static double getCurrentSpeedDisplayPercent(StatsData data) {
        return 100D + Math.max(0D, getMovementSoftCappedBonusPercent(getScaledMovementSpeedBonusPercent(data, getCurrentMovementSpeedValue(data))
                * DmzRevampConfig.REVAMP_MOVEMENT_SPEED_BONUS_MULTIPLIER.get()));
    }

    // Converts full-release SPD into the max movement percentage shown in the stats screen.
    public static double getMaxSpeedDisplayPercent(StatsData data) {
        return 100D + Math.max(0D, getMovementSoftCappedBonusPercent(getScaledMovementSpeedBonusPercent(data, getMaxMovementSpeedValue(data))
                * DmzRevampConfig.REVAMP_MOVEMENT_SPEED_BONUS_MULTIPLIER.get()));
    }

    // Reads the player's active SPD value; internally DMZ still stores it in SKP.
    public static double getCurrentSpeedValue(StatsData data) {
        return Math.max(0D, getCurrentStatFormula(data, "SKP"));
    }

    // Reads the player's full SPD value before power release is applied.
    public static double getMaxSpeedValue(StatsData data) {
        return Math.max(0D, getBaseStatFormula(data, "SKP"));
    }

    public static double getCurrentSpeedNoFormsValue(StatsData data) {
        return Math.max(0D, getBaseStatFormulaNoMultipliers(data, "SKP") * (data.getResources().getPowerRelease() / 100D));
    }

    /** Movement-family formulas use transformed base stats, but deliberately exclude every DMZ bonus entry. */
    public static double getCurrentMovementSpeedValue(StatsData data) {
        return Math.max(0D, getCurrentStatFormulaWithoutBonuses(data, "SKP"));
    }

    public static double getMaxMovementSpeedValue(StatsData data) {
        return Math.max(0D, getBaseStatFormulaWithoutBonuses(data, "SKP"));
    }

    public static double getCurrentMovementMeleeDamage(StatsData data) {
        return 1D + Math.max(0D, getCurrentStatFormulaWithoutBonuses(data, "STR"));
    }

    public static double getCurrentMovementKiDamage(StatsData data) {
        return 1D + Math.max(0D, getCurrentStatFormulaWithoutBonuses(data, "PWR"));
    }

    // Shows attack speed from effective Speed, reduced by effective Melee Damage.
    public static double getCurrentAttackSpeedDisplayPercent(StatsData data) {
        return Math.max(0D, 100D + getScaledAttackSpeedBonusPercent(data,
                getCurrentMovementSpeedValue(data),
                getCurrentMovementMeleeDamage(data)
        ));
    }

    // Shows swim speed from effective Speed and effective Melee Damage.
    public static double getCurrentSwimSpeedDisplayPercent(StatsData data) {
        double rawSwimSpeedBonusPercent = getScaledSwimSpeedBonusPercent(data,
                getCurrentMovementSpeedValue(data),
                getCurrentMovementMeleeDamage(data)
        ) * DmzRevampConfig.REVAMP_SWIM_SPEED_BONUS_MULTIPLIER.get();
        return 100D + Math.max(0D, getSwimSoftCappedBonusPercent(rawSwimSpeedBonusPercent));
    }

    public static double getSpeedStatProgress(StatsData data) {
        return getDefenseStyleSpeedReduction(data, 1D);
    }

    public static double getDefenseStyleSpeedReduction(StatsData data, double cap) {
        return getDefenseStyleStatReduction(data, "SKP", cap);
    }

    public static double getDefenseStyleStatReduction(StatsData data, String statName, double cap) {
        if (data == null || cap <= 0D) {
            return 0D;
        }
        String normalizedStat = statName == null ? "" : statName.toUpperCase();
        double maxStatValue = LevelingRevampConfig.levelsEnabled()
                ? com.dmzrevamp.revamp.prestige.PrestigeSystem.attributeFormulaMaximum()
                : getDmzUtilityStatReference(data);
        double expectedMaxStat = Math.max(1D, maxStatValue * data.getStatScaling(normalizedStat));
        // Like DMZ's no-form Defense reference, utility scaling accepts base-stat
        // bonuses but never transformation/stack-form multipliers.
        double stat = getBaseStatFormulaNoMultipliers(data, normalizedStat)
                * (data.getResources().getPowerRelease() / 100D);
        return getConfiguredDefenseStyleEffect(stat, expectedMaxStat, cap);
    }

    public static double getConfiguredDefenseStyleEffect(double stat, double expectedMaxStat, double cap) {
        if (cap <= 0D || expectedMaxStat <= 0D || !Double.isFinite(stat)) return 0D;
        if (DmzRevampConfig.CUSTOM_DEFENSE_AND_SPEED_EFFECTS_CURVE.get()) {
            return clamp(customDefenseAndSpeedCurveProgress(stat / expectedMaxStat) * cap, 0D, cap);
        }
        double kFactor = Math.max(12D,
                expectedMaxStat * ConfigManager.getCombatConfig().getDefenseReductionScale());
        double reduction = stat >= 0D ? stat / (kFactor + stat) : stat / (kFactor - stat);
        return clamp(reduction, 0D, cap);
    }

    public static double getDefenseCurveReference(StatsData data) {
        if (LevelingRevampConfig.levelsEnabled()) {
            return com.dmzrevamp.revamp.prestige.PrestigeSystem.attributeFormulaMaximum();
        }
        double configuredMax = Math.max(1D, data.getConfiguredMaxValue());
        return data.isMaxLevelValueInsteadOfStats() ? configuredMax * 6D / 2D : configuredMax;
    }

    private static double customDefenseAndSpeedCurveProgress(double ratio) {
        if (!Double.isFinite(ratio) || ratio <= 0D) return 0D;
        if (ratio <= 0.01D) return interpolate(ratio, 0D, 0.01D, 0D, 0.20D);
        if (ratio <= 0.10D) return interpolate(ratio, 0.01D, 0.10D, 0.20D, 0.40D);
        if (ratio <= 0.50D) return interpolate(ratio, 0.10D, 0.50D, 0.40D, 0.60D);
        if (ratio <= 1D) return interpolate(ratio, 0.50D, 1D, 0.60D, 1D);
        return 1D;
    }

    private static double interpolate(double value, double low, double high, double lowResult, double highResult) {
        double progress = (value - low) / Math.max(0.0000001D, high - low);
        return lowResult + (highResult - lowResult) * progress;
    }

    private static double getDmzUtilityStatReference(StatsData data) {
        double configuredMax = Math.max(1D, data.getConfiguredMaxValue());
        return data.isMaxLevelValueInsteadOfStats() ? configuredMax * 6D : configuredMax;
    }

    public static double getSpdCooldownReduction(StatsData data) {
        return getDefenseStyleSpeedReduction(data, DmzRevampConfig.SPD_COOLDOWN_REDUCTION_CAP.get());
    }

    public static double getSpdAttackSpeedIncrease(StatsData data) {
        return getDefenseStyleSpeedReduction(data, DmzRevampConfig.SPD_ATTACK_SPEED_INCREASE_CAP.get());
    }

    public static double getKiOverchargeCastTimeReduction(StatsData data) {
        double cap = DmzRevampConfig.SPD_ATTACK_SPEED_INCREASE_CAP.get();
        return getDefenseStyleStatReduction(data, "SKP", cap);
    }

    // Base conversion from SPD points to percentage before caps and movement-specific multipliers.
    public static double getScaledSpeedBonusPercent(double rawSkpValue) {
        return Math.max(0D, rawSkpValue * DmzRevampConfig.REVAMP_SPEED_PERCENT_PER_POINT.get());
    }

    public static double getScaledMovementSpeedBonusPercent(StatsData data, double rawSkpValue) {
        return getScaledSpeedBonusPercent(rawSkpValue) * getMovementStatCapScale(data);
    }

    // Swim speed uses the average of SPD and STR before the same percent conversion.
    public static double getScaledSwimSpeedBonusPercent(double rawSkpValue, double rawStrValue) {
        return getScaledPairedSpeedBonusPercent(rawSkpValue, rawStrValue);
    }

    public static double getScaledSwimSpeedBonusPercent(StatsData data, double rawSkpValue, double rawStrValue) {
        return getScaledPairedSpeedBonusPercent(data, rawSkpValue, rawStrValue);
    }

    public static double getScaledPairedSpeedBonusPercent(double speedValue, double pairedDamageValue) {
        return getScaledSpeedBonusPercent((Math.max(0D, speedValue) + Math.max(0D, pairedDamageValue)) / 2D);
    }

    public static double getScaledPairedSpeedBonusPercent(StatsData data, double speedValue, double pairedDamageValue) {
        return getScaledPairedSpeedBonusPercent(speedValue, pairedDamageValue) * getMovementStatCapScale(data);
    }

    public static double getMovementStatCapScale(StatsData data) {
        if (data == null) {
            return 1D;
        }
        int configuredMax = LevelingRevampConfig.levelsEnabled()
                ? com.dmzrevamp.revamp.prestige.PrestigeSystem.movementFormulaMaximum()
                : data.getConfiguredMaxValue();
        if (configuredMax <= 0) {
            return 1D;
        }
        return DmzRevampConfig.REVAMP_SPEED_MAX_LEVEL_SCALING_COEFFICIENT.get() / configuredMax;
    }

    public static double getScaledAttackSpeedBonusPercent(double rawSkpValue, double rawStrValue) {
        if (!DmzRevampConfig.ATTACK_SPEED_CHANGE.get()) {
            return 0D;
        }
        double speedBonus = getScaledSpeedBonusPercent(rawSkpValue);
        double strengthPenalty = DmzRevampConfig.ATTACK_SPEED_MELEE_DECREASE.get()
                ? getScaledSpeedBonusPercent(rawStrValue) * 0.5D : 0D;
        double minimumBonus = (DmzRevampConfig.MAX_ATTACK_SPEED_DECREASE_PERCENTAGE.get() - 1D) * 100D;
        return Math.max(minimumBonus, speedBonus - strengthPenalty);
    }

    public static double getScaledAttackSpeedBonusPercent(StatsData data, double rawSkpValue, double rawStrValue) {
        return getScaledAttackSpeedBonusPercent(rawSkpValue, rawStrValue) * getMovementStatCapScale(data);
    }

    // Returns the value used by getSoftCappedBonusPercent.
    public static double getSoftCappedBonusPercent(double rawBonusPercent) {
        return getSoftCappedBonusPercent(null, rawBonusPercent);
    }

    // Returns the value used by getSoftCappedBonusPercent.
    public static double getSoftCappedBonusPercent(Player player, double rawBonusPercent) {
        return getSoftCappedBonusPercent(rawBonusPercent, DmzRevampConfig.REVAMP_SPEED_SOFT_CAP_PERCENT.get() * getGravityMovementSpeedFactor(player));
    }

    // Returns the value used by getSoftCappedBonusPercent.
    public static double getSoftCappedBonusPercent(double rawBonusPercent, double softCapPercent) {
        double safeBonus = Math.max(0D, rawBonusPercent);
        double softCap = Math.max(0D, softCapPercent);
        if (safeBonus <= softCap) {
            return safeBonus;
        }

        double overflowEfficiency = DmzRevampConfig.REVAMP_SPEED_SOFT_CAP_OVERFLOW_EFFICIENCY.get();
        return softCap + ((safeBonus - softCap) * overflowEfficiency);
    }

    // Returns the movement-only soft cap. Swim and flight keep the configurable overflow efficiency.
    public static double getMovementSoftCappedBonusPercent(double rawBonusPercent) {
        return getSoftCappedBonusPercent(rawBonusPercent, DmzRevampConfig.REVAMP_MOVEMENT_SPEED_SOFT_CAP_PERCENT.get());
    }

    public static double getMovementSoftCappedBonusPercent(Player player, double rawBonusPercent) {
        return getSoftCappedBonusPercent(rawBonusPercent, DmzRevampConfig.REVAMP_MOVEMENT_SPEED_SOFT_CAP_PERCENT.get() * getGravityMovementSpeedFactor(player));
    }

    // Returns the swim-only soft cap.
    public static double getSwimSoftCappedBonusPercent(double rawBonusPercent) {
        return getSoftCappedBonusPercent(rawBonusPercent, DmzRevampConfig.REVAMP_SWIM_SPEED_SOFT_CAP_PERCENT.get());
    }

    public static double getSwimSoftCappedBonusPercent(Player player, double rawBonusPercent) {
        return getSoftCappedBonusPercent(rawBonusPercent, DmzRevampConfig.REVAMP_SWIM_SPEED_SOFT_CAP_PERCENT.get() * getGravityMovementSpeedFactor(player));
    }

    // Returns the flight-only soft cap.
    public static double getFlightSoftCappedBonusPercent(double rawBonusPercent) {
        return getSoftCappedBonusPercent(rawBonusPercent, DmzRevampConfig.REVAMP_FLIGHT_SPEED_SOFT_CAP_PERCENT.get());
    }

    public static double getFlightSoftCappedBonusPercent(Player player, double rawBonusPercent) {
        return getSoftCappedBonusPercent(rawBonusPercent, DmzRevampConfig.REVAMP_FLIGHT_SPEED_SOFT_CAP_PERCENT.get() * getGravityMovementSpeedFactor(player));
    }

    public static double getSearchFlightSoftCappedBonusPercent(double rawBonusPercent) {
        return getSoftCappedBonusPercent(rawBonusPercent, DmzRevampConfig.REVAMP_SEARCH_FLIGHT_SPEED_SOFT_CAP_PERCENT.get());
    }

    public static double getSearchFlightSoftCappedBonusPercent(Player player, double rawBonusPercent) {
        return getSoftCappedBonusPercent(rawBonusPercent, DmzRevampConfig.REVAMP_SEARCH_FLIGHT_SPEED_SOFT_CAP_PERCENT.get() * getGravityMovementSpeedFactor(player));
    }

    // Applies the acceleration ramp: speed above the base cap fades in over time instead of appearing instantly.
    public static double getRampedBonusPercent(double allowedBonusPercent, int activeTicks, boolean canKeepRamp) {
        return getRampedBonusPercent(null, allowedBonusPercent, activeTicks, canKeepRamp);
    }

    // Returns the value used by getRampedBonusPercent.
    public static double getRampedBonusPercent(Player player, double allowedBonusPercent, int activeTicks, boolean canKeepRamp) {
        return getRampedBonusPercent(player, allowedBonusPercent, activeTicks, canKeepRamp, DmzRevampConfig.REVAMP_SPEED_BASE_CAP_PERCENT.get());
    }

    public static double getRampedBonusPercent(Player player, double allowedBonusPercent, int activeTicks, boolean canKeepRamp, double baseCapPercent) {
        double sanitizedAllowedBonus = Math.max(0D, allowedBonusPercent);
        double baseCap = Math.max(0D, baseCapPercent * getGravityMovementSpeedFactor(player));
        if (sanitizedAllowedBonus <= baseCap) {
            return sanitizedAllowedBonus;
        }

        if (!canKeepRamp) {
            return baseCap;
        }

        double progress = Math.min(1D, Math.max(0, activeTicks) / (double) DmzRevampConfig.REVAMP_SPEED_RAMP_TICKS.get());
        double curvedProgress = progress * progress;
        return baseCap + ((sanitizedAllowedBonus - baseCap) * curvedProgress);
    }

    public static double getGravityMovementSpeedFactor(Player player) {
        return 1D - getGravityMovementPenalty(player);
    }

    public static double getGravityAttackSpeedFactor(Player player) {
        return 1D - getGravityAttackPenalty(player);
    }

    private static double getGravityMovementPenalty(Player player) {
        GeneralServerConfig.GravityConfig gravity = gravityConfig();
        if (player == null || gravity == null) {
            return 0D;
        }
        double gravityPenalty = gravityPenalty(player, gravity, true);
        double weightPenalty = WeightMovementPenaltyConfig.weightPenaltyOrOriginal(player);
        return clamp(gravityPenalty + weightPenalty, 0D, gravity.getMaxMovementPenalty());
    }

    private static double getGravityAttackPenalty(Player player) {
        GeneralServerConfig.GravityConfig gravity = gravityConfig();
        if (player == null || gravity == null) {
            return 0D;
        }
        double gravityPenalty = gravityPenalty(player, gravity, false);
        double weightPenalty = WeightMovementPenaltyConfig.weightPenaltyOrOriginal(player);
        return clamp(gravityPenalty + weightPenalty, 0D, gravity.getMaxAttackPenalty());
    }

    private static double gravityPenalty(Player player, GeneralServerConfig.GravityConfig gravity, boolean movement) {
        double penalizationGravity = GravityLogic.getPenalizationGravity(player);
        if (penalizationGravity <= 0D) {
            return 0D;
        }
        double maxPenalty = movement ? gravity.getMaxMovementPenalty() : gravity.getMaxAttackPenalty();
        if (penalizationGravity >= gravity.getHardStopThreshold()) {
            return maxPenalty;
        }

        // Mirrors DMZ's own curve so our speed caps shrink by the same percentage as the attribute penalty.
        double curve = movement
                ? GravityLogic.getGeneralPenaltyFactor(penalizationGravity)
                : Math.sqrt(penalizationGravity / 100D);
        return Math.min(maxPenalty, curve);
    }

    private static GeneralServerConfig.GravityConfig gravityConfig() {
        GeneralServerConfig serverConfig = ConfigManager.getServerConfig();
        return serverConfig == null ? null : serverConfig.getGravity();
    }

    private static double clamp01(double value) {
        return clamp(value, 0D, 1D);
    }

    private static double clamp(double value, double min, double max) {
        if (!Double.isFinite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    // Returns the value used by getRampDecayStep.
    public static int getRampDecayStep() {
        int rampTicks = Math.max(1, DmzRevampConfig.REVAMP_SPEED_RAMP_TICKS.get());
        int resetDelayTicks = Math.max(1, DmzRevampConfig.REVAMP_SPEED_RESET_DELAY_TICKS.get());
        return Math.max(1, (int) Math.ceil(rampTicks / (double) resetDelayTicks));
    }

    // Matches the server's configured rule for combining form, stack-form, and effect multipliers.
    public static double combineMultipliers(double formMultiplier, double stackMultiplier, double effectsMultiplier) {
        GeneralServerConfig serverConfig = ConfigManager.getServerConfig();
        boolean multiply = serverConfig != null
                && serverConfig.getGameplay() != null
                && Boolean.TRUE.equals(serverConfig.getGameplay().getMultiplicationInsteadOfAdditionForMultipliers());
        if (multiply) {
            return formMultiplier * stackMultiplier * effectsMultiplier;
        }
        return 1D + (formMultiplier - 1D) + (stackMultiplier - 1D) + (effectsMultiplier - 1D);
    }

    // Returns the value used by getSpecificFormMultiplier.
    public static double getSpecificFormMultiplier(StatsData data, String statName) {
        Character character = data.getCharacter();
        String currentForm = character.getActiveForm();
        String currentFormGroup = character.getActiveFormGroup();
        if (isBlank(currentForm) || isBlank(currentFormGroup)) {
            return 1D;
        }

        FormConfig formConfig = ConfigManager.getFormGroup(character.getRaceName(), currentFormGroup);
        if (formConfig == null) {
            return 1D;
        }

        FormConfig.FormData formData = formConfig.getForm(currentForm);
        if (formData == null) {
            return 1D;
        }

        return applyMasteryMultiplier(getBaseFormMultiplier(formData, statName), formData, character.getFormMasteries(), currentFormGroup, currentForm);
    }

    // Returns the value used by getSpecificStackFormMultiplier.
    public static double getSpecificStackFormMultiplier(StatsData data, String statName) {
        Character character = data.getCharacter();
        String currentForm = character.getActiveStackForm();
        String currentFormGroup = character.getActiveStackFormGroup();
        if (isBlank(currentForm) || isBlank(currentFormGroup)) {
            return 1D;
        }

        FormConfig formConfig = ConfigManager.getStackFormGroup(currentFormGroup);
        if (formConfig == null) {
            return 1D;
        }

        FormConfig.FormData formData = formConfig.getForm(currentForm);
        if (formData == null) {
            return 1D;
        }

        return applyMasteryMultiplier(getBaseFormMultiplier(formData, statName), formData, character.getStackFormMasteries(), currentFormGroup, currentForm);
    }

    // Scales a form multiplier upward as the player masters that form.
    private static double applyMasteryMultiplier(double baseMultiplier, FormConfig.FormData formData, FormMasteries masteries, String group, String form) {
        double mastery = masteries.getMastery(group, form);
        double maxMastery = Math.max(1D, getDouble(formData.getMaxMastery(), 100D));
        double maxStatsMultiplier = getDouble(formData.getMaxStatsMultiplier(), 1D);
        double masteryBonus = mastery * Math.max(0D, maxStatsMultiplier - 1D) / maxMastery;
        return 1D + ((baseMultiplier - 1D) * (1D + masteryBonus));
    }

    public static double getBaseStatFormulaNoMultipliers(StatsData data, String statName) {
        String normalizedStatName = statName.toUpperCase();
        double statValue = switch (normalizedStatName) {
            case "STR" -> data.getStats().getStrength();
            case "SKP" -> data.getStats().getStrikePower();
            case "RES" -> data.getStats().getResistance();
            case "VIT" -> data.getStats().getVitality();
            case "PWR" -> data.getStats().getKiPower();
            case "ENE" -> data.getStats().getEnergy();
            default -> 0D;
        };
        double statBonus = data.getBonusStats().calculateBonus(normalizedStatName, (int) Math.round(statValue), false);
        double multipliedStatBonus = data.getBonusStats().calculateBonus(normalizedStatName, (int) Math.round(statValue), true);
        return ((statValue + multipliedStatBonus) * data.getStatScaling(normalizedStatName))
                + (statBonus * data.getStatScaling(normalizedStatName));
    }

    public static double getCurrentStatFormulaWithoutBonuses(StatsData data, String statName) {
        return getBaseStatFormulaWithoutBonuses(data, statName)
                * (data.getResources().getPowerRelease() / 100D);
    }

    public static double getBaseStatFormulaWithoutBonuses(StatsData data, String statName) {
        String normalizedStatName = statName.toUpperCase();
        double statValue = switch (normalizedStatName) {
            case "STR" -> data.getStats().getStrength();
            case "SKP" -> data.getStats().getStrikePower();
            case "RES" -> data.getStats().getResistance();
            case "VIT" -> data.getStats().getVitality();
            case "PWR" -> data.getStats().getKiPower();
            case "ENE" -> data.getStats().getEnergy();
            default -> 0D;
        };
        return statValue * data.getStatScaling(normalizedStatName) * data.getTotalMultiplier(normalizedStatName);
    }

    // Returns the value used by getBaseFormMultiplier.
    private static double getBaseFormMultiplier(FormConfig.FormData formData, String statName) {
        return switch (statName.toUpperCase()) {
            case "STR" -> getDouble(formData.getStrMultiplier(), 1D);
            case "SKP" -> getDouble(formData.getSkpMultiplier(), 1D);
            case "DEF" -> getDouble(formData.getDefMultiplier(), 1D);
            case "STM" -> getDouble(formData.getStmMultiplier(), 1D);
            case "VIT" -> getDouble(formData.getVitMultiplier(), 1D);
            case "PWR" -> getDouble(formData.getPwrMultiplier(), 1D);
            case "ENE" -> getDouble(formData.getEneMultiplier(), 1D);
            default -> 1D;
        };
    }

    // Returns the value used by isBlank.
    private static boolean isBlank(String value) {
        return value == null || value.isEmpty();
    }

    // Returns the value used by getInt.
    private static int getInt(Integer value) {
        return value == null ? 0 : value;
    }

    // Returns the value used by getDouble.
    private static double getDouble(Double value, double fallback) {
        return value == null ? fallback : value;
    }

    // Handles the clampToInt logic for this class.
    private static int clampToInt(long value) {
        if (value < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) value;
    }
}
