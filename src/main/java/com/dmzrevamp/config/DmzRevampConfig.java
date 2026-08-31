package com.dmzrevamp.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class DmzRevampConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.DoubleValue REVAMP_SPEED_BASE_CAP_PERCENT = BUILDER
            .comment("Default extra speed cap percent before sprint or movement ramping increases it.")
            .defineInRange("revamp.speed.baseCapPercent", 100D, 0D, 1000000D);

    public static final ForgeConfigSpec.BooleanValue ENABLE_SPD_MOVEMENT_SPEED_MODIFIERS = BUILDER
            .comment("When false, SPD does not modify movement, swim, flight, step height, or fluid running speed.")
            .define("revamp.speed.enableMovementSpeedModifiers", true);

    public static final ForgeConfigSpec.BooleanValue COMBAT_FLIGHT_TRIDIMENSIONAL = BUILDER
            .comment("When true, Combat Flight movement while locked on follows the target in three dimensions. When false, locked Combat Flight keeps Dragon Mine Z's original movement.")
            .define("combatFlightTridimensional", true);

    public static final ForgeConfigSpec.BooleanValue ALLOW_LOCK_ON_ANDROID = BUILDER
            .comment("When true, Lock On can target Android Upgraded players. When false, Dragon Mine Z's original Android targeting restriction is preserved.")
            .define("allowLockOnAndroid", true);

    public static final ForgeConfigSpec.DoubleValue REVAMP_SPEED_PERCENT_PER_POINT = BUILDER
            .comment("Base speed percentage gained per point of Speed before movement-type formulas, caps, and multipliers are applied. 0.5 means each SPD grants +0.5% speed.")
            .defineInRange("revamp.speed.basePercentPerSpeed", 0.5D, 0D, 1_000_000D);

    public static final ForgeConfigSpec.BooleanValue ATTACK_SPEED_CHANGE = BUILDER
            .comment("When false, Overhaul does not change player attack speed through Speed or Melee Damage.")
            .define("revamp.speed.AttackSpeedChange", true);

    public static final ForgeConfigSpec.BooleanValue ATTACK_SPEED_MELEE_DECREASE = BUILDER
            .comment("When true, Melee Damage reduces the attack speed granted by Speed. When false, only Speed controls attack speed.")
            .define("revamp.speed.attackSpeedMeleeDecrease", true);

    public static final ForgeConfigSpec.DoubleValue MAX_ATTACK_SPEED_DECREASE_PERCENTAGE = BUILDER
            .comment("Lowest attack-speed multiplier Overhaul may produce. 0.5 means no less than 50 percent of normal attack speed.")
            .defineInRange("revamp.speed.maxAttackSpeedDecreasePercentage", 0.5D, 0D, 1D);

    public static final ForgeConfigSpec.DoubleValue REVAMP_SPEED_MAX_LEVEL_SCALING_COEFFICIENT = BUILDER
            .comment("This value divided by the DMZ max level (thisValue / maxLevel) determines the general speed gain multiplier. Examples: 10000 with max level 10000 = x1.0; 10000 with max level 1000000 = x0.01.")
            .defineInRange("revamp.speed.maxLevelScalingCoefficient", 10000D, 0D, 1_000_000_000D);

    public static final ForgeConfigSpec.DoubleValue REVAMP_MOVEMENT_SPEED_BONUS_MULTIPLIER = BUILDER
            .comment("Multiplier applied to SPD movement speed bonus. 1.0 = 100 percent.")
            .defineInRange("revamp.speed.movementBonusMultiplier", 1D, 0D, 100D);

    public static final ForgeConfigSpec.DoubleValue REVAMP_SWIM_SPEED_BONUS_MULTIPLIER = BUILDER
            .comment("Multiplier applied to SPD swim speed bonus. 0.5 = 50 percent.")
            .defineInRange("revamp.speed.swimBonusMultiplier", 0.5D, 0D, 100D);

    public static final ForgeConfigSpec.DoubleValue REVAMP_FLIGHT_SPEED_BONUS_MULTIPLIER = BUILDER
            .comment("Multiplier applied to SPD combat flight speed bonus. 1.0 = 100 percent.")
            .defineInRange("revamp.speed.combatFlightBonusMultiplier", 1.0D, 0D, 100D);

    public static final ForgeConfigSpec.DoubleValue REVAMP_SEARCH_FLIGHT_SPEED_BONUS_MULTIPLIER = BUILDER
            .comment("Multiplier applied to SPD search flight speed bonus. 0.3 = 30 percent.")
            .defineInRange("revamp.speed.searchFlightBonusMultiplier", 0.3D, 0D, 100D);

    public static final ForgeConfigSpec.DoubleValue REVAMP_SWIM_SPEED_BASE_CAP_PERCENT = BUILDER
            .comment("Extra swim speed cap percent before sprint or movement ramping increases it.")
            .defineInRange("revamp.speed.swimBaseCapPercent", 100D, 0D, 1000000D);

    public static final ForgeConfigSpec.DoubleValue REVAMP_FLIGHT_SPEED_BASE_CAP_PERCENT = BUILDER
            .comment("Extra combat flight speed cap percent before fast flight movement ramping increases it.")
            .defineInRange("revamp.speed.combatFlightBaseCapPercent", 200D, 0D, 1000000D);

    public static final ForgeConfigSpec.DoubleValue REVAMP_SEARCH_FLIGHT_SPEED_BASE_CAP_PERCENT = BUILDER
            .comment("Extra search flight speed cap percent before fast flight movement ramping increases it.")
            .defineInRange("revamp.speed.searchFlightBaseCapPercent", 50D, 0D, 1000000D);

    public static final ForgeConfigSpec.IntValue REVAMP_SPEED_RAMP_TICKS = BUILDER
            .comment("Ticks required to ramp speed caps from the base cap to the full allowed bonus.")
            .defineInRange("revamp.speed.rampTicks", 100, 1, 1000000);

    public static final ForgeConfigSpec.IntValue REVAMP_SPEED_RESET_DELAY_TICKS = BUILDER
            .comment("Ticks after stopping movement or sprint before the extra speed cap snaps back to the base cap.")
            .defineInRange("revamp.speed.resetDelayTicks", 20, 0, 1000000);

    public static final ForgeConfigSpec.DoubleValue REVAMP_SPEED_SOFT_CAP_PERCENT = BUILDER
            .comment("Soft cap threshold for speed bonus percent. Bonus above this threshold is reduced by the overflow efficiency.")
            .defineInRange("revamp.speed.softCapPercent", 900D, 0D, 1000000D);

    public static final ForgeConfigSpec.DoubleValue REVAMP_MOVEMENT_SPEED_SOFT_CAP_PERCENT = BUILDER
            .comment("Soft cap threshold for running movement speed bonus percent. Bonus above this threshold is reduced by the overflow efficiency.")
            .defineInRange("revamp.speed.movementSoftCapPercent", 1500D, 0D, 1000000D);

    public static final ForgeConfigSpec.DoubleValue REVAMP_SWIM_SPEED_SOFT_CAP_PERCENT = BUILDER
            .comment("Soft cap threshold for swim speed bonus percent. Bonus above this threshold is reduced by the overflow efficiency.")
            .defineInRange("revamp.speed.swimSoftCapPercent", 500D, 0D, 1000000D);

    public static final ForgeConfigSpec.DoubleValue REVAMP_FLIGHT_SPEED_SOFT_CAP_PERCENT = BUILDER
            .comment("Soft cap threshold for combat flight speed bonus percent. Bonus above this threshold is reduced by the overflow efficiency.")
            .defineInRange("revamp.speed.combatFlightSoftCapPercent", 1500D, 0D, 1000000D);

    public static final ForgeConfigSpec.DoubleValue REVAMP_SEARCH_FLIGHT_SPEED_SOFT_CAP_PERCENT = BUILDER
            .comment("Soft cap threshold for search flight speed bonus percent. Bonus above this threshold is reduced by the overflow efficiency.")
            .defineInRange("revamp.speed.searchFlightSoftCapPercent", 200D, 0D, 1000000D);

    public static final ForgeConfigSpec.DoubleValue REVAMP_SPEED_SOFT_CAP_OVERFLOW_EFFICIENCY = BUILDER
            .comment("Efficiency applied to speed bonus percent above the soft cap. 0.005 means 0.5 percent efficiency after the soft cap.")
            .defineInRange("revamp.speed.softCapOverflowEfficiency", 0.00025D, 0D, 1D);

    public static final ForgeConfigSpec.DoubleValue SPD_COOLDOWN_REDUCTION_CAP = BUILDER
            .comment("Maximum cooldown reduction from SPD. 0.7 = 70 percent.")
            .defineInRange("combat.spdCooldownReductionCap", 0.75D, 0D, 1D);

    public static final ForgeConfigSpec.DoubleValue SPD_ATTACK_SPEED_INCREASE_CAP = BUILDER
            .comment("Maximum Ki attack speed and Strike dash distance increase from SPD. 0.7 = 70 percent.")
            .defineInRange("combat.spdAttackSpeedIncreaseCap", 0.75D, 0D, 100D);

    public static final ForgeConfigSpec.BooleanValue CUSTOM_DEFENSE_AND_SPEED_EFFECTS_CURVE = BUILDER
            .comment("When true, Defense damage reduction and Speed-based cooldown, cast time, Ki attack speed, and Strike dash distance effects use the custom progression curve: 1% of the reference stat grants 20% of the configured cap, 10% grants 40%, 50% grants 60%, and 100% grants 100%. When false, the original DMZ-style hyperbolic curve is used.")
            .define("combat.customDefenseAndSpeedEffectsCurve", true);

    public static final ForgeConfigSpec.BooleanValue ENABLE_DYNAMIC_GROWTH_NOTIFICATION_SUPPRESSION = BUILDER
            .comment("When true, Dynamic Growth stat gain messages become less frequent after the configured stat threshold.")
            .define("dynamicGrowth.enableNotificationSuppression", true);

    public static final ForgeConfigSpec.IntValue DYNAMIC_GROWTH_NOTIFICATION_SUPPRESSION_START = BUILDER
            .comment("Current stat value where Dynamic Growth messages start using milestone intervals. 1000 means messages show every 10 points from 1000 to 9999, then every 100 points from 10000 to 99999, and so on.")
            .defineInRange("dynamicGrowth.notificationSuppressionStart", 1000, 1, 1_000_000_000);

    public static final ForgeConfigSpec.DoubleValue CUSTOM_DYNAMIC_GROWTH_FIXED_XP_PER_UNIT = BUILDER
            .comment("Fixed Dynamic XP granted per unit by Overhaul custom actions. 0.5 is half of the former value 1.0.")
            .defineInRange("dynamicGrowth.customActions.fixedXpPerUnit", 0.5D, 0D, 1_000_000D);

    public static final ForgeConfigSpec.DoubleValue CUSTOM_DYNAMIC_GROWTH_STAT_PERCENT_PER_UNIT = BUILDER
            .comment("Fraction of the relevant stat added as Dynamic XP per unit by Overhaul custom actions. 0.005 means 0.5 percent, half of the former 1 percent.")
            .defineInRange("dynamicGrowth.customActions.statFractionPerUnit", 0.005D, 0D, 100D);

    public static final ForgeConfigSpec.DoubleValue CUSTOM_DYNAMIC_GROWTH_OTHER_ACTION_MULTIPLIER = BUILDER
            .comment("Multiplier for Overhaul custom Dynamic Growth actions that are not unit-based movement, such as block, parry, dodge, counter and gravity training.")
            .defineInRange("dynamicGrowth.customActions.otherActionMultiplier", 0.5D, 0D, 100D);

    public static final ForgeConfigSpec.IntValue MAX_MASTER_WEIGHT = BUILDER
            .comment("Maximum weight value that master NPCs can give to a player.")
            .defineInRange("training.maxMasterWeight", 1_000_000_000, 1, 1_000_000_000);

    public static final ForgeConfigSpec.DoubleValue KI_OVERCHARGE_DESTRUCTION_MULTIPLIER_CAP = BUILDER
            .comment("Maximum final destruction/explosion multiplier allowed for overcharged Ki attacks. 2.0 = up to two times normal destruction.")
            .defineInRange("kiAttacks.overchargeDestructionMultiplierCap", 2D, 1D, 100D);

    public static final ForgeConfigSpec.BooleanValue CAP_KI_OVERCHARGE_COOLDOWN_TO_NORMAL_DMZ_RELEASE = BUILDER
            .comment("When true and dmzkiovercharge is not loaded, Ki attack cooldown from extended overcharge is capped to DMZ's normal 175 percent release limit.")
            .define("kiAttacks.capOverchargeCooldownToNormalDmzRelease", true);

    public static final ForgeConfigSpec.BooleanValue ENABLE_SPD_PLAYER_EVASION = BUILDER
            .comment("When true, player targets can evade player attacks based on SPD comparison.")
            .define("combat.enableSpdPlayerEvasion", true);

    public static final ForgeConfigSpec.DoubleValue SPD_PLAYER_EVASION_MAX_CHANCE_PERCENT = BUILDER
            .comment("Maximum player evasion chance from SPD comparison. 50 = 50 percent.")
            .defineInRange("combat.spdPlayerEvasionMaxChancePercent", 50D, 0D, 100D);

    public static final ForgeConfigSpec.BooleanValue ENABLE_KI_ATTACK_CATEGORY_EQUIP_LIMITS = BUILDER
            .comment("When true, limits how many Advanced and Ultimate Ki attacks can be equipped.")
            .define("kiAttacks.enableCategoryEquipLimits", true);

    public static final ForgeConfigSpec.IntValue MAX_EQUIPPED_ADVANCED_KI_ATTACKS = BUILDER
            .comment("Maximum equipped Advanced Ki attacks when category equip limits are enabled.")
            .defineInRange("kiAttacks.maxEquippedAdvanced", 2, 1, 8);

    public static final ForgeConfigSpec.IntValue MAX_EQUIPPED_ULTIMATE_KI_ATTACKS = BUILDER
            .comment("Maximum equipped Ultimate Ki attacks when category equip limits are enabled.")
            .defineInRange("kiAttacks.maxEquippedUltimate", 2, 1, 8);

    public static final ForgeConfigSpec.BooleanValue RECALCULATE_DMZ_ENTITY_BATTLE_POWER = BUILDER
            .comment("When true, Dragon Mine Z saga/manual BP entities with already-set BP have that BP recalculated from their current attributes using the player-style curved BP formula. Normal mobs that use DMZ's mob BP helper always use the custom formula. Entities with exactly Integer.MAX_VALUE BP keep DMZ's hidden BP behavior.")
            .define("battlePower.recalculateDmzEntityBattlePower", true);

    public static final ForgeConfigSpec.EnumValue<HelmetKeepHairListMode> HELMET_KEEP_HAIR_LIST_MODE = BUILDER
            .comment("Controls how Dragon Mine Z's helmetsThatKeepHair list is interpreted. WHITELIST keeps DMZ's default behavior. BLACKLIST makes listed helmets hide hair while all unlisted helmets keep hair.")
            .defineEnum("revamp.helmetKeepHairListMode", HelmetKeepHairListMode.BLACKLIST);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    // Creates a new DmzRevampConfig instance.
    private DmzRevampConfig() {
    }

    public enum HelmetKeepHairListMode {
        WHITELIST,
        BLACKLIST
    }
}
