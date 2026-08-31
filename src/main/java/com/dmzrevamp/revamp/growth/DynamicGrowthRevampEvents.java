package com.dmzrevamp.revamp.growth;

import com.dmzrevamp.DmzRevampMod;
import com.dmzrevamp.config.DynamicGrowthCurveConfig;
import com.dmzrevamp.config.DmzRevampConfig;
import com.dmzrevamp.revamp.DmzRevampHelper;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.extras.DynamicGrowthStat;
import com.dragonminez.server.dynamicgrowth.DynamicGrowthService;
import com.dragonminez.server.util.GravityLogic;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

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
        award(player, data, DynamicGrowthStat.SKP, otherXp(DmzRevampHelper.getCurrentSpeedValue(data) * PERFECT_DODGE_SPEED_RATIO), attacker);
    }

    public static void awardPerfectCounter(ServerPlayer player) {
        StatsData data = getData(player);
        if (data == null) {
            return;
        }
        award(player, data, DynamicGrowthStat.SKP, otherXp(DmzRevampHelper.getCurrentSpeedValue(data) * PERFECT_COUNTER_SPEED_RATIO), null);
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

        if (player.isSwimming() && player.isInWater()) {
            awardWholeBlocks(player, data, state, fullDistance, true);
            state.runningDistance = 0D;
            state.flyingDistance = 0D;
        } else if (isFastFlying(player, data, fullDistance)) {
            awardFastFlightWholeBlocks(player, data, state, fullDistance);
            state.runningDistance = 0D;
            state.swimmingDistance = 0D;
        } else if (player.isSprinting() && !player.isInWater()) {
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
                award(player, data, DynamicGrowthStat.RES, xpForUnits(1D, data.getMaxStamina()), null);
            }
        } else {
            state.underwaterTicks = 0;
        }

        if (player.isInLava()) {
            state.lavaTicks++;
            if (state.lavaTicks >= RESISTANCE_INTERVAL_TICKS) {
                state.lavaTicks -= RESISTANCE_INTERVAL_TICKS;
                award(player, data, DynamicGrowthStat.RES, xpForUnits(1D, data.getMaxStamina()) * LAVA_RESISTANCE_MULTIPLIER, null);
            }
        } else {
            state.lavaTicks = 0;
        }

        boolean flying = isFlying(player, data);
        boolean movedWhileFlying = Double.isFinite(fullDistance)
                && (fullDistance * fullDistance) > MIN_FLIGHT_MOVEMENT_SQR;
        if (flying && movedWhileFlying) {
            state.flightEnergyTicks++;
            if (state.flightEnergyTicks >= FLIGHT_ENERGY_INTERVAL_TICKS) {
                state.flightEnergyTicks -= FLIGHT_ENERGY_INTERVAL_TICKS;
                award(player, data, DynamicGrowthStat.ENE, xpForUnits(1D, stat(data, "ENE")), null);
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
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        STATES.remove(event.getEntity().getUUID());
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
            award(player, data, DynamicGrowthStat.STR, xpForUnits(blocks, stat(data, "STR")), null);
            award(player, data, DynamicGrowthStat.SKP, xpForUnits(blocks, DmzRevampHelper.getCurrentSpeedValue(data)), null);
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
        award(player, data, DynamicGrowthStat.SKP, xpForUnits(blocks, DmzRevampHelper.getCurrentSpeedValue(data)), null);
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
        double speedXp = xpForUnits(blocks, DmzRevampHelper.getCurrentSpeedValue(data)) * 0.5D;
        double kiDamageXp = xpForUnits(blocks, data.getKiDamage()) * 0.5D;
        // Fast flight trains speed from SPD and ki output from the player's effective Ki Damage.
        award(player, data, DynamicGrowthStat.SKP, speedXp, null);
        award(player, data, DynamicGrowthStat.PWR, kiDamageXp, null);
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
        DynamicGrowthAwardContext.runWithTpMultiplier(weightTpMultiplier, () -> {
            award(player, data, DynamicGrowthStat.STR, gravityXp(data.getMeleeDamage(), xpRatio, trainingGravity), null);
            award(player, data, DynamicGrowthStat.SKP, gravityXp(data.getStrikeDamage(), xpRatio, trainingGravity), null);
            award(player, data, DynamicGrowthStat.RES, gravityXp((data.getDefense() + data.getMaxStamina()) * 0.5D, xpRatio, trainingGravity), null);
            award(player, data, DynamicGrowthStat.VIT, gravityXp(data.getMaxHealth(), xpRatio, trainingGravity), null);
            award(player, data, DynamicGrowthStat.PWR, gravityXp(data.getKiDamage(), xpRatio, trainingGravity), null);
            award(player, data, DynamicGrowthStat.ENE, gravityXp(data.getMaxEnergy(), xpRatio, trainingGravity), null);
        });
    }

    private static double gravityXp(double derivedStatValue, double xpRatio, double trainingGravity) {
        // Gravity trains what the stat currently produces in gameplay, not the raw stat point number.
        return Math.max(0D, derivedStatValue) * xpRatio * trainingGravity
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

    private static double stat(StatsData data, String statName) {
        return data == null ? 0D : DmzRevampHelper.getCurrentStatFormula(data, statName);
    }

    private static void award(ServerPlayer player, StatsData data, DynamicGrowthStat stat, double xp, LivingEntity target) {
        if (xp > 0D && Double.isFinite(xp)) {
            // All custom growth rewards go through DMZ's service so TP multipliers and notifications stay centralized.
            DynamicGrowthService.award(player, data, stat, xp, target);
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
