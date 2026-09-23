package com.dmzrevamp.revamp.growth;

import com.dmzrevamp.DmzRevampMod;
import com.dmzrevamp.config.DynamicGrowthCurveConfig;
import com.dmzrevamp.config.DmzRevampConfig;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.init.MainAttributes;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.extras.DynamicGrowthStat;
import com.dragonminez.server.dynamicgrowth.DynamicGrowthService;
import com.dragonminez.server.util.GravityLogic;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DynamicGrowthRevampEvents {
    private static final double PERFECT_DODGE_SPEED_RATIO = 0.5D;
    private static final double PERFECT_COUNTER_SPEED_RATIO = 1D;
    private static final double BLOCK_DAMAGE_RATIO = 0.5D;
    private static final double PARRY_BONUS_RATIO = 1.5D;
    private static final double LAVA_RESISTANCE_MULTIPLIER = 2D;
    private static final double FAST_FLIGHT_GROWTH_SPEED_SQR = 0.55D * 0.55D;
    private static final int RESISTANCE_INTERVAL_TICKS = 100;
    private static final int FLIGHT_ENERGY_INTERVAL_TICKS = 20;
    private static final double MIN_FLIGHT_MOVEMENT_SQR = 1.0E-8D;
    private static final int GRAVITY_GROWTH_INTERVAL_TICKS = 20;
    private static final Map<UUID, MovementGrowthState> STATES = new ConcurrentHashMap<>();
    private static final Map<UUID, HungerSnapshot> HUNGER_SNAPSHOTS = new ConcurrentHashMap<>();

    private DynamicGrowthRevampEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPerfectEvasion(DMZEvent.PlayerEvasionEvent event) {
        awardPerfectDodge(event.getPlayer(), event.getAttacker());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerBlock(DMZEvent.PlayerBlockEvent event) {
        StatsData data = getData(event.getVictim());
        if (data == null) {
            return;
        }

        double xp = otherXp(Math.max(0D, event.getOriginalDamage()) * BLOCK_DAMAGE_RATIO);
        if (event.isParry()) {
            xp *= PARRY_BONUS_RATIO;
        }
        award(event.getVictim(), data, DynamicGrowthStat.RES, xp, event.getAttacker());
    }

    public static void awardPerfectDodge(ServerPlayer player, LivingEntity attacker) {
        StatsData data = getData(player);
        if (data == null) {
            return;
        }
        award(player, data, DynamicGrowthStat.SKP, otherXp(dynamicStatReference(data, "SKP") * PERFECT_DODGE_SPEED_RATIO), attacker);
    }

    public static void awardPerfectCounter(ServerPlayer player) {
        StatsData data = getData(player);
        if (data == null) {
            return;
        }
        award(player, data, DynamicGrowthStat.SKP, otherXp(dynamicStatReference(data, "SKP") * PERFECT_COUNTER_SPEED_RATIO), null);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide() || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        StatsData data = getData(player);
        if (data == null || !data.getStatus().isHasCreatedCharacter()) {
            STATES.remove(player.getUUID());
            return;
        }

        MovementGrowthState state = STATES.computeIfAbsent(player.getUUID(), ignored -> new MovementGrowthState(player.position()));
        Vec3 currentPosition = player.position();
        double horizontalDistance = horizontalDistance(state.lastPosition, currentPosition);
        double fullDistance = state.lastPosition.distanceTo(currentPosition);
        state.lastPosition = currentPosition;

        boolean mounted = player.isPassenger();
        if (!mounted && player.isSwimming() && player.isInWater()) {
            awardWholeBlocks(player, data, state, fullDistance, true);
            state.runningDistance = 0D;
            state.flyingDistance = 0D;
        } else if (!mounted && isFastFlying(player, data, fullDistance)) {
            awardFastFlightWholeBlocks(player, data, state, fullDistance);
            state.runningDistance = 0D;
            state.swimmingDistance = 0D;
        } else if (!mounted && player.isSprinting() && !player.isInWater()) {
            awardSkpWholeBlocks(player, data, state, horizontalDistance, MovementMode.RUNNING);
            state.swimmingDistance = 0D;
            state.flyingDistance = 0D;
        } else {
            state.runningDistance = 0D;
            state.swimmingDistance = 0D;
            state.flyingDistance = 0D;
        }

        if (isUsingOxygenUnderwater(player)) {
            state.underwaterTicks++;
            if (state.underwaterTicks >= RESISTANCE_INTERVAL_TICKS) {
                state.underwaterTicks -= RESISTANCE_INTERVAL_TICKS;
                award(player, data, DynamicGrowthStat.RES, xpForUnits(1D, staminaReference(player, data)), null);
            }
        } else {
            state.underwaterTicks = 0;
        }

        if (player.isInLava()) {
            state.lavaTicks++;
            if (state.lavaTicks >= RESISTANCE_INTERVAL_TICKS) {
                state.lavaTicks -= RESISTANCE_INTERVAL_TICKS;
                award(player, data, DynamicGrowthStat.RES, xpForUnits(1D, staminaReference(player, data)) * LAVA_RESISTANCE_MULTIPLIER, null);
            }
        } else {
            state.lavaTicks = 0;
        }

        boolean flying = !mounted && isFlying(player, data);
        boolean movedWhileFlying = Double.isFinite(fullDistance)
                && (fullDistance * fullDistance) > MIN_FLIGHT_MOVEMENT_SQR;
        if (flying && movedWhileFlying) {
            state.flightEnergyTicks++;
            if (state.flightEnergyTicks >= FLIGHT_ENERGY_INTERVAL_TICKS) {
                state.flightEnergyTicks -= FLIGHT_ENERGY_INTERVAL_TICKS;
                award(player, data, DynamicGrowthStat.ENE, xpForUnits(1D, dynamicStatReference(data, "ENE")), null);
            }
        } else if (!flying) {
            state.flightEnergyTicks = 0;
        }

        state.gravityGrowthTicks++;
        if (state.gravityGrowthTicks >= GRAVITY_GROWTH_INTERVAL_TICKS) {
            state.gravityGrowthTicks -= GRAVITY_GROWTH_INTERVAL_TICKS;
            awardGravityGrowth(player, data);
        }
    }

    @SubscribeEvent
    public static void onFoodUseStart(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        FoodProperties food = event.getItem().getFoodProperties(player);
        if (food == null || food.getNutrition() <= 0) {
            HUNGER_SNAPSHOTS.remove(player.getUUID());
            return;
        }

        HUNGER_SNAPSHOTS.put(player.getUUID(), HungerSnapshot.capture(player));
    }

    @SubscribeEvent
    public static void onFoodUseStop(LivingEntityUseItemEvent.Stop event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HUNGER_SNAPSHOTS.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onFoodUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        HungerSnapshot hungerBefore = HUNGER_SNAPSHOTS.remove(player.getUUID());
        FoodProperties food = event.getItem().getFoodProperties(player);
        if (food == null || food.getNutrition() <= 0) {
            return;
        }

        StatsData data = getData(player);
        if (data == null || !data.getStatus().isHasCreatedCharacter()) {
            return;
        }

        // Nutrition is the food's Hunger Points. Saturation is intentionally ignored.
        double xp = xpForUnits(food.getNutrition(), vitalityReference(player, data));
        if (hungerBefore != null && hungerBefore.wasAppliedOrRefreshed(player)) {
            xp *= 0.5D;
        }
        award(player, data, DynamicGrowthStat.VIT, xp, null);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID playerId = event.getEntity().getUUID();
        STATES.remove(playerId);
        HUNGER_SNAPSHOTS.remove(playerId);
    }

    private static void awardWholeBlocks(ServerPlayer player, StatsData data, MovementGrowthState state, double distance, boolean swimming) {
        if (distance <= 0D || !Double.isFinite(distance)) {
            return;
        }

        if (swimming) {
            state.swimmingDistance += distance;
            int blocks = (int) state.swimmingDistance;
            if (blocks <= 0) {
                return;
            }
            state.swimmingDistance -= blocks;
            award(player, data, DynamicGrowthStat.STR, xpForUnits(blocks, dynamicStatReference(data, "STR")), null);
            award(player, data, DynamicGrowthStat.SKP, xpForUnits(blocks, dynamicStatReference(data, "SKP")), null);
        } else {
            awardSkpWholeBlocks(player, data, state, distance, MovementMode.RUNNING);
        }
    }

    private static void awardSkpWholeBlocks(ServerPlayer player, StatsData data, MovementGrowthState state, double distance, MovementMode mode) {
        if (distance <= 0D || !Double.isFinite(distance)) {
            return;
        }

        double total = mode == MovementMode.FLYING ? state.flyingDistance + distance : state.runningDistance + distance;
        int blocks = (int) total;
        if (blocks <= 0) {
            if (mode == MovementMode.FLYING) {
                state.flyingDistance = total;
            } else {
                state.runningDistance = total;
            }
            return;
        }

        double remainder = total - blocks;
        if (mode == MovementMode.FLYING) {
            state.flyingDistance = remainder;
        } else {
            state.runningDistance = remainder;
        }
        award(player, data, DynamicGrowthStat.SKP, xpForUnits(blocks, dynamicStatReference(data, "SKP")), null);
    }

    private static void awardFastFlightWholeBlocks(ServerPlayer player, StatsData data, MovementGrowthState state, double distance) {
        if (distance <= 0D || !Double.isFinite(distance)) {
            return;
        }

        state.flyingDistance += distance;
        int blocks = (int) state.flyingDistance;
        if (blocks <= 0) {
            return;
        }

        state.flyingDistance -= blocks;
        double speedXp = xpForUnits(blocks, dynamicStatReference(data, "SKP")) * 0.5D;
        double powerXp = xpForUnits(blocks, dynamicStatReference(data, "PWR")) * 0.5D;
        // Fast flight trains the raw distributed SKP/PWR after race+class scaling only.
        award(player, data, DynamicGrowthStat.SKP, speedXp, null);
        award(player, data, DynamicGrowthStat.PWR, powerXp, null);
    }

    private static void awardGravityGrowth(ServerPlayer player, StatsData data) {
        double weightTpMultiplier = data.getTpWeightBellMultiplier();
        if (weightTpMultiplier <= 1D || !Double.isFinite(weightTpMultiplier)) {
            return;
        }

        double trainingGravity = GravityLogic.getTrainingBonusGravity(player);
        double xpRatio = DynamicGrowthCurveConfig.xpPercentagePerGravityPerSecond();
        if (trainingGravity <= 0D || xpRatio <= 0D || !Double.isFinite(trainingGravity) || !Double.isFinite(xpRatio)) {
            return;
        }

        // Gravity growth is caused by training weight, so it only uses DMZ's weight TP multiplier.
        // Every stat reference below is rebuilt from the distributed stat and its race/class scale only.
        // RES keeps the existing DEF/Stamina average, while VIT/Stamina/Energy add only their base max attribute.
        double defenseReference = dynamicStatReference(data, "DEF");
        double staminaReference = staminaReference(player, data);
        DynamicGrowthAwardContext.runWithTpMultiplier(weightTpMultiplier, () -> {
            award(player, data, DynamicGrowthStat.STR, gravityXp(dynamicStatReference(data, "STR"), xpRatio, trainingGravity), null);
            award(player, data, DynamicGrowthStat.SKP, gravityXp(dynamicStatReference(data, "SKP"), xpRatio, trainingGravity), null);
            award(player, data, DynamicGrowthStat.RES, gravityXp((defenseReference + staminaReference) * 0.5D, xpRatio, trainingGravity), null);
            award(player, data, DynamicGrowthStat.VIT, gravityXp(vitalityReference(player, data), xpRatio, trainingGravity), null);
            award(player, data, DynamicGrowthStat.PWR, gravityXp(dynamicStatReference(data, "PWR"), xpRatio, trainingGravity), null);
            award(player, data, DynamicGrowthStat.ENE, gravityXp(maxEnergyReference(player, data), xpRatio, trainingGravity), null);
        });
    }

    private static double gravityXp(double statReference, double xpRatio, double trainingGravity) {
        return Math.max(0D, statReference) * xpRatio * trainingGravity
                * DmzRevampConfig.CUSTOM_DYNAMIC_GROWTH_OTHER_ACTION_MULTIPLIER.get();
    }

    private static double otherXp(double xp) {
        return xp * DmzRevampConfig.CUSTOM_DYNAMIC_GROWTH_OTHER_ACTION_MULTIPLIER.get();
    }

    private static boolean isFlying(ServerPlayer player, StatsData data) {
        return player.hasEffect(MainEffects.FLY.get()) && data.getSkills().isSkillActive("fly");
    }

    private static boolean isFastFlying(ServerPlayer player, StatsData data, double distanceThisTick) {
        if (!isFlying(player, data)) {
            return false;
        }
        if (data.getStatus().getFlightMode() == 1) {
            return true;
        }
        // Search Flight can move the player without a reliable server velocity, so measured distance is used too.
        return player.isSprinting()
                && ((distanceThisTick * distanceThisTick) > FAST_FLIGHT_GROWTH_SPEED_SQR
                || player.getDeltaMovement().lengthSqr() > FAST_FLIGHT_GROWTH_SPEED_SQR);
    }

    private static boolean isUsingOxygenUnderwater(ServerPlayer player) {
        return player.isUnderWater() && player.getAirSupply() < player.getMaxAirSupply();
    }

    private static double horizontalDistance(Vec3 from, Vec3 to) {
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        return Math.sqrt((dx * dx) + (dz * dz));
    }

    private static StatsData getData(ServerPlayer player) {
        return StatsProvider.get(StatsCapability.INSTANCE, player).resolve().orElse(null);
    }

    private static double xpForUnits(double units, double statValue) {
        double safeUnits = Double.isFinite(units) ? Math.max(0D, units) : 0D;
        double safeStat = Double.isFinite(statValue) ? Math.max(0D, statValue) : 0D;
        return safeUnits * (DmzRevampConfig.CUSTOM_DYNAMIC_GROWTH_FIXED_XP_PER_UNIT.get()
                + safeStat * DmzRevampConfig.CUSTOM_DYNAMIC_GROWTH_STAT_PERCENT_PER_UNIT.get());
    }

    private static double dynamicStatReference(StatsData data, String statName) {
        if (data == null || statName == null) {
            return 0D;
        }

        String key = statName.toUpperCase(Locale.ROOT);
        double rawStat;
        String scalingKey;
        switch (key) {
            case "STR" -> {
                rawStat = data.getStats().getStrength();
                scalingKey = "STR";
            }
            case "SKP" -> {
                rawStat = data.getStats().getStrikePower();
                scalingKey = "SKP";
            }
            case "DEF" -> {
                rawStat = data.getStats().getResistance();
                scalingKey = "DEF";
            }
            case "STM" -> {
                rawStat = data.getStats().getResistance();
                scalingKey = "STM";
            }
            case "VIT" -> {
                rawStat = data.getStats().getVitality();
                scalingKey = "VIT";
            }
            case "PWR" -> {
                rawStat = data.getStats().getKiPower();
                scalingKey = "PWR";
            }
            case "ENE" -> {
                rawStat = data.getStats().getEnergy();
                scalingKey = "ENE";
            }
            default -> {
                return 0D;
            }
        }

        double value = rawStat * data.getStatScaling(scalingKey);
        return Double.isFinite(value) ? Math.max(0D, value) : 0D;
    }

    private static double staminaReference(ServerPlayer player, StatsData data) {
        return dynamicStatReference(data, "STM")
                + baseAttributeValue(player, MainAttributes.MAX_STAMINA.get(), 20D);
    }

    private static double maxEnergyReference(ServerPlayer player, StatsData data) {
        return dynamicStatReference(data, "ENE")
                + baseAttributeValue(player, MainAttributes.MAX_ENERGY.get(), 20D);
    }

    private static double vitalityReference(ServerPlayer player, StatsData data) {
        return dynamicStatReference(data, "VIT")
                + baseAttributeValue(player, Attributes.MAX_HEALTH, 20D);
    }

    private static double baseAttributeValue(ServerPlayer player, Attribute attribute, double fallback) {
        if (player == null || attribute == null) {
            return fallback;
        }
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return fallback;
        }
        double value = instance.getBaseValue();
        return Double.isFinite(value) ? Math.max(0D, value) : fallback;
    }

    private static void award(ServerPlayer player, StatsData data, DynamicGrowthStat stat, double xp, LivingEntity target) {
        if (xp > 0D && Double.isFinite(xp)) {
            // All custom growth rewards go through DMZ's service so TP multipliers and notifications stay centralized.
            DynamicGrowthService.award(player, data, stat, xp, target);
        }
    }

    private static final class HungerSnapshot {
        private final boolean active;
        private final int amplifier;
        private final int duration;
        private final int startTick;

        private HungerSnapshot(boolean active, int amplifier, int duration, int startTick) {
            this.active = active;
            this.amplifier = amplifier;
            this.duration = duration;
            this.startTick = startTick;
        }

        private static HungerSnapshot capture(ServerPlayer player) {
            MobEffectInstance hunger = player.getEffect(MobEffects.HUNGER);
            if (hunger == null) {
                return new HungerSnapshot(false, -1, 0, player.tickCount);
            }
            return new HungerSnapshot(true, hunger.getAmplifier(), hunger.getDuration(), player.tickCount);
        }

        private boolean wasAppliedOrRefreshed(ServerPlayer player) {
            MobEffectInstance hungerAfter = player.getEffect(MobEffects.HUNGER);
            if (hungerAfter == null) {
                return false;
            }
            if (!active) {
                return true;
            }
            if (hungerAfter.getAmplifier() > amplifier) {
                return true;
            }
            if (hungerAfter.getAmplifier() < amplifier) {
                return false;
            }

            int elapsedTicks = Math.max(0, player.tickCount - startTick);
            int expectedRemaining = Math.max(0, duration - elapsedTicks);
            return hungerAfter.getDuration() > expectedRemaining + 1;
        }
    }

    private static final class MovementGrowthState {
        private Vec3 lastPosition;
        private double runningDistance;
        private double swimmingDistance;
        private double flyingDistance;
        private int underwaterTicks;
        private int lavaTicks;
        private int flightEnergyTicks;
        private int gravityGrowthTicks;

        private MovementGrowthState(Vec3 lastPosition) {
            this.lastPosition = lastPosition;
        }
    }

    private enum MovementMode {
        RUNNING,
        FLYING
    }
}
