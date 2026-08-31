package com.dmzrevamp.revamp;

import com.dmzrevamp.config.DmzRevampConfig;
import com.dmzrevamp.DmzRevampMod;
import com.dmzrevamp.revamp.speed.SpeedLimitData;
import com.dragonminez.common.init.EntityAttributes;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DmzSpeedRevampEvents {
    private static final UUID SPD_MOVE_SPEED_UUID = UUID.fromString("a7b80f30-b2d0-4a24-94fd-81d5d76f3b11");
    private static final UUID SPD_ATTACK_SPEED_UUID = UUID.fromString("8ca68ce2-6e0f-4f3d-bb0d-bf8d138f95f3");
    private static final UUID SPD_SWIM_SPEED_UUID = UUID.fromString("4f9f0e9f-06f2-4aa0-a8b9-a9daea3a2f92");
    private static final UUID SPD_STEP_HEIGHT_UUID = UUID.fromString("6cf39c32-85b9-4a7e-a7b5-9a06dd3fbe8a");
    private static final UUID FLY_SPEED_UUID = UUID.fromString("47ba2127-fb49-4f7b-8f7c-8e16e830b8d3");
    private static final UUID DMZ_SPRINT_SPEED_UUID = UUID.fromString("c4c4e8b0-5f21-4f16-9a2d-123456789abc");
    // Vanilla's sprint modifier must remain outside Overhaul's Speed Limit.
    private static final UUID VANILLA_SPRINT_SPEED_UUID = UUID.fromString("662a6b8d-da3e-4c1c-8813-96ea6097278d");
    public static final float DEFAULT_CREATIVE_FLY_SPEED = 0.05F;
    private static final double STEP_HEIGHT_SPEED_INTERVAL_PERCENT = 100D;
    private static final double FLUID_RUN_TOTAL_SPEED_THRESHOLD_PERCENT = 500D;
    private static final double SEARCH_FAST_FLIGHT_THRESHOLD_SQR = 0.55D * 0.55D;
    private static final double FLUID_RUN_STEP_DISTANCE = 0.6D;
    public static final String EMPTY_ATTACK_SPRINT_GRACE_TAG = "dmzrevamp_empty_attack_sprint_grace";
    public static final int EMPTY_ATTACK_SPRINT_GRACE_TICKS = 8;
    private static final Map<UUID, SpeedState> SPEED_STATE = new ConcurrentHashMap<>();
    private static final Map<UUID, Double> LAST_FLY_SPEED = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> FLIGHT_MOVEMENT_TICKS = new ConcurrentHashMap<>();
    private static final Map<UUID, FluidRunVisualState> FLUID_RUN_VISUAL_STATE = new ConcurrentHashMap<>();

    // Forge calls the static event methods directly, so this event holder should not be instantiated.
    private DmzSpeedRevampEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    // Recalculates movement, attack speed, swim speed, flight speed, and fluid running after DMZ updates player stats.
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide() || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            if (!data.getStatus().isHasCreatedCharacter()) {
                clearSpeedModifiers(player);
                clearFlySpeedModifier(player);
                SPEED_STATE.remove(player.getUUID());
                LAST_FLY_SPEED.remove(player.getUUID());
                FLIGHT_MOVEMENT_TICKS.remove(player.getUUID());
                FLUID_RUN_VISUAL_STATE.remove(player.getUUID());
                return;
            }

            cleanupTransformationEffects(player, data);
            if (DmzRevampConfig.ENABLE_SPD_MOVEMENT_SPEED_MODIFIERS.get()) {
                applySpeedRevamp(player, data);
                applyFlightSpeedRevamp(player, data);
            } else {
                clearSpeedModifiers(player);
                clearFlySpeedModifier(player);
                SPEED_STATE.remove(player.getUUID());
                LAST_FLY_SPEED.remove(player.getUUID());
                FLIGHT_MOVEMENT_TICKS.remove(player.getUUID());
                FLUID_RUN_VISUAL_STATE.remove(player.getUUID());
            }
            suppressSprintExhaustion(player);
        });
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    // Removes temporary speed modifiers and cached ramp state when the player leaves.
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearSpeedModifiers(player);
            clearFlySpeedModifier(player);
            SPEED_STATE.remove(player.getUUID());
            LAST_FLY_SPEED.remove(player.getUUID());
            FLIGHT_MOVEMENT_TICKS.remove(player.getUUID());
            FLUID_RUN_VISUAL_STATE.remove(player.getUUID());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    // Invalidates transient speed modifier caches after Minecraft replaces the player entity.
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SPEED_STATE.remove(player.getUUID());
            LAST_FLY_SPEED.remove(player.getUUID());
            FLIGHT_MOVEMENT_TICKS.remove(player.getUUID());
            FLUID_RUN_VISUAL_STATE.remove(player.getUUID());
        }
    }

    // Kept for compatibility with older calls; Overhaul now treats flight boost as automatic while flying.
    public static void toggleFlightBoostMode(ServerPlayer player) {
        // Flight boost is always active while the player is flying.
    }

    // Returns the currently applied movement bonus after ramping, or zero if the player has no cached speed state.
    public static double getCurrentMovementSpeedBonusPercent(ServerPlayer player) {
        SpeedState state = SPEED_STATE.get(player.getUUID());
        return state == null || Double.isNaN(state.effectiveMoveSpeedBonusPercent) ? 0D : Math.max(0D, state.effectiveMoveSpeedBonusPercent);
    }

    // Calculates the highest movement bonus this player can reach while sprinting long enough to finish the ramp.
    public static double getMaxRunningMovementSpeedBonusPercent(ServerPlayer player, StatsData data) {
        double rawSpeedBonusPercent = getRawMovementSpeedBonusPercent(player, data);
        double movementSoftCappedBonusPercent = DmzRevampHelper.getMovementSoftCappedBonusPercent(player, rawSpeedBonusPercent);
        return sanitizePercent(DmzRevampHelper.getRampedBonusPercent(player, movementSoftCappedBonusPercent, DmzRevampConfig.REVAMP_SPEED_RAMP_TICKS.get(), true));
    }

    // Calculates raw movement from the effective Speed value before soft caps and ramping.
    public static double getUncappedMovementSpeedBonusPercent(StatsData data) {
        return sanitizePercent(DmzRevampHelper.getScaledMovementSpeedBonusPercent(data, DmzRevampHelper.getCurrentMovementSpeedValue(data)));
    }

    // Calculates active attack speed from effective Speed and Melee Damage.
    public static double getAttackSpeedBonusPercent(StatsData data) {
        return sanitizeSignedPercent(DmzRevampHelper.getScaledAttackSpeedBonusPercent(data,
                DmzRevampHelper.getCurrentMovementSpeedValue(data),
                DmzRevampHelper.getCurrentMovementMeleeDamage(data)
        ));
    }

    // Returns the value used by getActiveFlightSpeed.
    public static float getActiveFlightSpeed(Player player, StatsData data, int movementTicks) {
        return (float) (DEFAULT_CREATIVE_FLY_SPEED * getActiveFlightSpeedMultiplier(player, data, movementTicks));
    }

    // Returns the value used by getActiveFlightSpeedMultiplier.
    public static double getActiveFlightSpeedMultiplier(Player player, StatsData data, int movementTicks) {
        boolean combatFlight = data != null && data.getStatus().getFlightMode() == 1;
        return getActiveFlightSpeedMultiplier(player, data, movementTicks, combatFlight);
    }

    public static double getActiveCombatFlightSpeedMultiplier(Player player, StatsData data, int movementTicks) {
        return getActiveFlightSpeedMultiplier(player, data, movementTicks, true);
    }

    public static double getActiveSearchFlightSpeedMultiplier(Player player, StatsData data, int movementTicks) {
        return getActiveFlightSpeedMultiplier(player, data, movementTicks, false);
    }

    private static double getActiveFlightSpeedMultiplier(Player player, StatsData data, int movementTicks, boolean combatFlight) {
        if (!DmzRevampConfig.ENABLE_SPD_MOVEMENT_SPEED_MODIFIERS.get()) {
            return 1D;
        }
        double rawFlightBonus = hasPlayerSpeedLimit(data)
                ? getOverhaulFlightBonusPercent(data, combatFlight)
                : getRawFlightBonusPercent(player, data, combatFlight);
        double allowedBonusPercent = applyPlayerSpeedLimit(data,
                getFlightSoftCappedBonusPercent(player, rawFlightBonus, combatFlight));
        double effectiveBonusPercent = getRampedFlightBonusPercent(player, allowedBonusPercent, movementTicks, true, combatFlight);
        return Math.max(0D, (1D + (effectiveBonusPercent / 100D)) * DmzRevampHelper.getGravityMovementSpeedFactor(player));
    }

    // Returns the value used by getMaximumFlightSpeedDisplayPercent.
    public static double getMaximumFlightSpeedDisplayPercent(Player player, StatsData data) {
        boolean combatFlight = data != null && data.getStatus().getFlightMode() == 1;
        return getMaximumFlightSpeedDisplayPercent(player, data, combatFlight);
    }

    public static double getMaximumCombatFlightSpeedDisplayPercent(Player player, StatsData data) {
        return getMaximumFlightSpeedDisplayPercent(player, data, true);
    }

    public static double getMaximumSearchFlightSpeedDisplayPercent(Player player, StatsData data) {
        return getMaximumFlightSpeedDisplayPercent(player, data, false);
    }

    private static double getMaximumFlightSpeedDisplayPercent(Player player, StatsData data, boolean combatFlight) {
        if (!DmzRevampConfig.ENABLE_SPD_MOVEMENT_SPEED_MODIFIERS.get()) {
            return 100D;
        }
        double rawFlightBonus = hasPlayerSpeedLimit(data)
                ? getOverhaulFlightBonusPercent(data, combatFlight)
                : getRawFlightBonusPercent(player, data, combatFlight);
        double allowedBonusPercent = applyPlayerSpeedLimit(data,
                getFlightSoftCappedBonusPercent(player, rawFlightBonus, combatFlight));
        return 100D + Math.max(0D, allowedBonusPercent);
    }

    // Returns the value used by getCurrentMovementSpeedBonusPercent.
    public static double getCurrentMovementSpeedBonusPercent(Player player, StatsData data, int rampTicks) {
        if (!DmzRevampConfig.ENABLE_SPD_MOVEMENT_SPEED_MODIFIERS.get()) {
            return 0D;
        }
        double rawSpeedBonusPercent = getRawMovementSpeedBonusPercent(player, data);
        double movementSoftCappedBonusPercent = DmzRevampHelper.getMovementSoftCappedBonusPercent(player, rawSpeedBonusPercent);
        return sanitizePercent(DmzRevampHelper.getRampedBonusPercent(player, movementSoftCappedBonusPercent, rampTicks, true));
    }

    // Returns the value used by getStepHeightAdditionForSpeed.
    public static double getStepHeightAdditionForSpeed(double movementSpeedBonusPercent) {
        return Math.min(64D, Math.floor(Math.max(0D, movementSpeedBonusPercent) / STEP_HEIGHT_SPEED_INTERVAL_PERCENT));
    }

    // Lets very fast sprinting players skim across water or lava instead of sinking immediately.
    public static void applyFluidRun(Player player, double totalMovementSpeedPercent) {
        if (totalMovementSpeedPercent < FLUID_RUN_TOTAL_SPEED_THRESHOLD_PERCENT
                || !player.isSprinting()
                || player.isShiftKeyDown()
                || player.isSwimming()
                || player.isPassenger()) {
            clearFluidRunVisuals(player);
            return;
        }

        BlockPos fluidPos = findRunnableFluidSurface(player);
        if (fluidPos == null) {
            clearFluidRunVisuals(player);
            return;
        }

        Level level = player.level();
        FluidState fluidState = level.getFluidState(fluidPos);
        double surfaceY = fluidPos.getY() + fluidState.getHeight(level, fluidPos);
        double playerY = player.getY();
        if (playerY < surfaceY - 0.08D || playerY > surfaceY + 0.4D) {
            clearFluidRunVisuals(player);
            return;
        }

        Vec3 movement = player.getDeltaMovement();
        if (movement.y < 0D) {
            player.setDeltaMovement(movement.x, 0D, movement.z);
        }
        if (playerY < surfaceY) {
            player.setPos(player.getX(), surfaceY, player.getZ());
        }
        player.setOnGround(true);
        player.fallDistance = 0F;
        emitFluidRunStepEffects(player, fluidState, surfaceY);
    }

    // Syncs vanilla/Forge attributes with Overhaul's SPD rules for ground movement, attack speed, swimming, and step height.
    private static void applySpeedRevamp(ServerPlayer player, StatsData data) {
        AttributeInstance moveSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        AttributeInstance swimSpeed = player.getAttribute(ForgeMod.SWIM_SPEED.get());
        AttributeInstance stepHeight = player.getAttribute(ForgeMod.STEP_HEIGHT_ADDITION.get());
        if (moveSpeed == null || attackSpeed == null || swimSpeed == null || stepHeight == null) {
            return;
        }

        SpeedState state = SPEED_STATE.computeIfAbsent(player.getUUID(), ignored -> new SpeedState());
        // These derived stats are comparatively expensive; calculate each once for all movement branches in this tick.
        double currentSpeed = DmzRevampHelper.getCurrentMovementSpeedValue(data);
        double meleeDamage = DmzRevampHelper.getCurrentMovementMeleeDamage(data);
        double externalMovementMultiplier = Math.max(1D, getExternalMovementSpeedMultiplier(moveSpeed));
        double externalSwimMultiplier = Math.max(0.01D, getExternalSwimSpeedMultiplier(swimSpeed));
        double rawMovementSpeedBonusPercent = sanitizePercent(
                DmzRevampHelper.getScaledMovementSpeedBonusPercent(data, currentSpeed)
                        * DmzRevampConfig.REVAMP_MOVEMENT_SPEED_BONUS_MULTIPLIER.get()
                        + Math.max(0D, (externalMovementMultiplier - 1D) * 100D));
        double rawAttackSpeedBonusPercent = sanitizeSignedPercent(DmzRevampHelper.getScaledAttackSpeedBonusPercent(data,
                currentSpeed,
                meleeDamage
        ));
        double overhaulSwimSpeedBonusPercent = sanitizePercent(DmzRevampHelper.getScaledSwimSpeedBonusPercent(data,
                currentSpeed,
                meleeDamage
        ) * DmzRevampConfig.REVAMP_SWIM_SPEED_BONUS_MULTIPLIER.get());
        double rawSwimSpeedBonusPercent = hasPlayerSpeedLimit(data)
                ? overhaulSwimSpeedBonusPercent
                : overhaulSwimSpeedBonusPercent + Math.max(0D, (externalSwimMultiplier - 1D) * 100D);

        double attackSpeedCapPercent = Math.max(0D, DmzRevampConfig.SPD_ATTACK_SPEED_INCREASE_CAP.get() * 100D * DmzRevampHelper.getGravityAttackSpeedFactor(player));
        if (rawAttackSpeedBonusPercent > attackSpeedCapPercent) {
            rawAttackSpeedBonusPercent = attackSpeedCapPercent;
        }
        double softCappedSwimBonusPercent = applyPlayerSpeedLimit(data,
                DmzRevampHelper.getSwimSoftCappedBonusPercent(player, rawSwimSpeedBonusPercent));

        if (shouldCountAsSprintingForRamp(player)) {
            state.speedRampTicks = Math.min(DmzRevampConfig.REVAMP_SPEED_RAMP_TICKS.get(), state.speedRampTicks + 1);
        } else if (state.speedRampTicks > 0) {
            state.speedRampTicks = Math.max(0, state.speedRampTicks - DmzRevampHelper.getRampDecayStep());
        }

        double movementSoftCappedBonusPercent = applyPlayerSpeedLimit(data,
                DmzRevampHelper.getMovementSoftCappedBonusPercent(player, rawMovementSpeedBonusPercent));
        double movementSpeedBonusPercent = sanitizePercent(DmzRevampHelper.getRampedBonusPercent(
                player, movementSoftCappedBonusPercent, state.speedRampTicks, true));
        state.effectiveMoveSpeedBonusPercent = movementSpeedBonusPercent;
        double movementModifierAmount = ((1D + (movementSpeedBonusPercent / 100D)) / externalMovementMultiplier) - 1D;
        state.moveSpeedBonus = syncModifierAllowNegative(
                moveSpeed,
                SPD_MOVE_SPEED_UUID,
                "DMZ SPD movement bonus",
                movementModifierAmount,
                state.moveSpeedBonus
        );
        state.attackSpeedBonus = syncModifier(
                attackSpeed,
                SPD_ATTACK_SPEED_UUID,
                "DMZ SPD attack bonus",
                DmzRevampConfig.ATTACK_SPEED_CHANGE.get() ? rawAttackSpeedBonusPercent / 100D : 0D,
                state.attackSpeedBonus,
                AttributeModifier.Operation.MULTIPLY_TOTAL,
                true
        );
        double rampedSwimBonusPercent = sanitizePercent(DmzRevampHelper.getRampedBonusPercent(
                player,
                softCappedSwimBonusPercent,
                state.speedRampTicks,
                true,
                DmzRevampConfig.REVAMP_SWIM_SPEED_BASE_CAP_PERCENT.get()
        ));
        double swimModifierAmount = hasPlayerSpeedLimit(data)
                ? rampedSwimBonusPercent / 100D
                : ((1D + (rampedSwimBonusPercent / 100D)) / Math.max(1D, externalSwimMultiplier)) - 1D;
        state.swimSpeedBonus = syncModifier(
                swimSpeed,
                SPD_SWIM_SPEED_UUID,
                "DMZ SPD swim bonus",
                swimModifierAmount,
                state.swimSpeedBonus,
                AttributeModifier.Operation.MULTIPLY_TOTAL,
                true
        );
        state.stepHeightAddition = syncModifier(
                stepHeight,
                SPD_STEP_HEIGHT_UUID,
                "DMZ SPD step height bonus",
                getStepHeightAdditionForSpeed(movementSpeedBonusPercent),
                state.stepHeightAddition,
                AttributeModifier.Operation.ADDITION
        );
        applyFluidRun(player, 100D + movementSpeedBonusPercent);
    }

    // Syncs DMZ flight speed with the current SPD/PWR-based flight ramp.
    private static void applyFlightSpeedRevamp(ServerPlayer player, StatsData data) {
        UUID playerId = player.getUUID();
        if (!player.hasEffect(MainEffects.FLY.get())) {
            clearFlySpeedModifier(player);
            LAST_FLY_SPEED.remove(playerId);
            FLIGHT_MOVEMENT_TICKS.remove(playerId);
            return;
        }

        trackFlightMovement(player);
        int movementTicks = FLIGHT_MOVEMENT_TICKS.getOrDefault(playerId, 0);
        double targetFlySpeedBonus = getActiveFlightSpeedMultiplier(player, data, movementTicks) - 1D;
        double previousSpeed = LAST_FLY_SPEED.getOrDefault(playerId, Double.NaN);
        if (Math.abs(previousSpeed - targetFlySpeedBonus) > 0.0001D) {
            syncFlySpeedModifier(player, targetFlySpeedBonus);
        }
        LAST_FLY_SPEED.put(playerId, targetFlySpeedBonus);
    }

    // Handles the syncModifier logic for this class.
    private static double syncModifier(AttributeInstance attribute, UUID uuid, String name, double amount, double previousAmount) {
        return syncModifier(attribute, uuid, name, amount, previousAmount, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    // Handles the syncModifier logic for this class.
    private static double syncModifier(AttributeInstance attribute, UUID uuid, String name, double amount, double previousAmount, AttributeModifier.Operation operation) {
        return syncModifier(attribute, uuid, name, amount, previousAmount, operation, false);
    }

    // Handles the syncModifier logic for this class.
    private static double syncModifier(AttributeInstance attribute, UUID uuid, String name, double amount, double previousAmount, AttributeModifier.Operation operation, boolean allowNegative) {
        double sanitizedAmount = allowNegative
                ? (Double.isFinite(amount) ? Math.max(-0.99D, amount) : 0D)
                : (amount > 0D && Double.isFinite(amount) ? amount : 0D);
        boolean shouldHaveModifier = allowNegative ? Math.abs(sanitizedAmount) > 0.000001D : sanitizedAmount > 0D;
        AttributeModifier existing = attribute.getModifier(uuid);
        if (!Double.isNaN(previousAmount)
                && Math.abs(previousAmount - sanitizedAmount) <= 0.0001D
                && (shouldHaveModifier == (existing != null))) {
            return sanitizedAmount;
        }

        removeModifier(attribute, uuid);
        if (shouldHaveModifier) {
            attribute.addTransientModifier(new AttributeModifier(uuid, name, sanitizedAmount, operation));
        }
        return sanitizedAmount;
    }

    // Handles modifier synchronization for values that can be negative, such as compensation for external speed modifiers.
    private static double syncModifierAllowNegative(AttributeInstance attribute, UUID uuid, String name, double amount, double previousAmount) {
        double sanitizedAmount = Double.isFinite(amount) ? Math.max(-0.99D, amount) : 0D;
        boolean shouldHaveModifier = Math.abs(sanitizedAmount) > 0.000001D;
        AttributeModifier existing = attribute.getModifier(uuid);
        if (!Double.isNaN(previousAmount)
                && Math.abs(previousAmount - sanitizedAmount) <= 0.0001D
                && (shouldHaveModifier == (existing != null))) {
            return sanitizedAmount;
        }

        removeModifier(attribute, uuid);
        if (shouldHaveModifier) {
            attribute.addTransientModifier(new AttributeModifier(uuid, name, sanitizedAmount, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
        return sanitizedAmount;
    }

    // Removes only the transient attribute modifiers owned by Overhaul's SPD system.
    private static void clearSpeedModifiers(ServerPlayer player) {
        removeModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), SPD_MOVE_SPEED_UUID);
        removeModifier(player.getAttribute(Attributes.ATTACK_SPEED), SPD_ATTACK_SPEED_UUID);
        removeModifier(player.getAttribute(ForgeMod.SWIM_SPEED.get()), SPD_SWIM_SPEED_UUID);
        removeModifier(player.getAttribute(ForgeMod.STEP_HEIGHT_ADDITION.get()), SPD_STEP_HEIGHT_UUID);
    }

    // Replaces the old flight modifier with the newly calculated multiplier.
    private static void syncFlySpeedModifier(ServerPlayer player, double amount) {
        AttributeInstance flySpeed = player.getAttribute(EntityAttributes.FLY_SPEED.get());
        if (flySpeed == null) {
            return;
        }
        AttributeModifier existing = flySpeed.getModifier(FLY_SPEED_UUID);
        if (existing != null) {
            flySpeed.removeModifier(existing);
        }
        double sanitizedAmount = Double.isFinite(amount) ? Math.max(-0.99D, amount) : 0D;
        if (Math.abs(sanitizedAmount) > 0.000001D) {
            flySpeed.addTransientModifier(new AttributeModifier(FLY_SPEED_UUID, "Dragon Mine Z: Overhaul flight speed", sanitizedAmount, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    // Removes Overhaul's flight-speed modifier while leaving other mods' modifiers alone.
    private static void clearFlySpeedModifier(ServerPlayer player) {
        removeModifier(player.getAttribute(EntityAttributes.FLY_SPEED.get()), FLY_SPEED_UUID);
    }

    // Removes a known modifier id from an attribute if that attribute exists on the player.
    private static void removeModifier(AttributeInstance attribute, UUID uuid) {
        if (attribute == null) {
            return;
        }
        AttributeModifier modifier = attribute.getModifier(uuid);
        if (modifier != null) {
            attribute.removeModifier(modifier);
        }
    }

    // Clears stale DMZ transformation effects when the character no longer has matching active form data.
    private static void cleanupTransformationEffects(ServerPlayer player, StatsData data) {
        if (!data.getCharacter().hasActiveForm()) {
            player.removeEffect(MainEffects.TRANSFORMED.get());
        }
        if (!data.getCharacter().hasActiveStackForm()) {
            player.removeEffect(MainEffects.STACK_TRANSFORMED.get());
        }
    }

    // Stops vanilla sprint hunger drain so movement balance comes from SPD and Ki systems instead.
    private static void suppressSprintExhaustion(ServerPlayer player) {
        if (player.isSprinting() && player.getFoodData().getExhaustionLevel() > 0F) {
            player.getFoodData().setExhaustion(0F);
        }
    }

    // Keeps the server-side SPD ramp stable while an empty DMZ melee swing corrects sprint state on the client.
    private static boolean shouldCountAsSprintingForRamp(ServerPlayer player) {
        if (player.isSprinting()) {
            player.getPersistentData().remove(EMPTY_ATTACK_SPRINT_GRACE_TAG);
            return true;
        }

        int graceTicks = player.getPersistentData().getInt(EMPTY_ATTACK_SPRINT_GRACE_TAG);
        if (graceTicks <= 0) {
            return false;
        }

        if (graceTicks == 1) {
            player.getPersistentData().remove(EMPTY_ATTACK_SPRINT_GRACE_TAG);
        } else {
            player.getPersistentData().putInt(EMPTY_ATTACK_SPRINT_GRACE_TAG, graceTicks - 1);
        }
        return true;
    }

    // Handles the sanitizePercent logic for this class.
    private static double sanitizePercent(double value) {
        return Double.isFinite(value) && value > 0D ? value : 0D;
    }

    private static double sanitizeSignedPercent(double value) {
        return Double.isFinite(value) ? value : 0D;
    }

    // Looks just below the player's feet for water or lava that can become a temporary running surface.
    private static BlockPos findRunnableFluidSurface(Player player) {
        BlockPos feet = BlockPos.containing(player.getX(), player.getY() - 0.05D, player.getZ());
        if (isRunnableFluid(player.level(), feet)) {
            return feet;
        }

        BlockPos lowerFeet = BlockPos.containing(player.getX(), player.getY() - 0.35D, player.getZ());
        return isRunnableFluid(player.level(), lowerFeet) ? lowerFeet : null;
    }

    // Accepts only real water or lava fluid states as surfaces for high-speed running.
    private static boolean isRunnableFluid(Level level, BlockPos pos) {
        FluidState fluidState = level.getFluidState(pos);
        return !fluidState.isEmpty() && (fluidState.is(FluidTags.WATER) || fluidState.is(FluidTags.LAVA));
    }

    // Emits block-step style particles and dripstone step sounds while SPD lets the player run on fluids.
    private static void emitFluidRunStepEffects(Player player, FluidState fluidState, double surfaceY) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        UUID playerId = player.getUUID();
        FluidRunVisualState state = FLUID_RUN_VISUAL_STATE.computeIfAbsent(playerId, ignored -> new FluidRunVisualState(player.getX(), player.getZ()));
        double dx = player.getX() - state.lastX;
        double dz = player.getZ() - state.lastZ;
        state.lastX = player.getX();
        state.lastZ = player.getZ();

        double horizontalDistance = Math.sqrt((dx * dx) + (dz * dz));
        if (!Double.isFinite(horizontalDistance) || horizontalDistance <= 0.001D) {
            return;
        }

        state.stepDistance += horizontalDistance;
        if (state.stepDistance < FLUID_RUN_STEP_DISTANCE) {
            return;
        }
        state.stepDistance %= FLUID_RUN_STEP_DISTANCE;

        boolean lava = fluidState.is(FluidTags.LAVA);
        BlockState particleState = lava ? Blocks.LAVA.defaultBlockState() : Blocks.WATER.defaultBlockState();
        SoundEvent stepSound = lava ? SoundEvents.POINTED_DRIPSTONE_DRIP_LAVA : SoundEvents.POINTED_DRIPSTONE_DRIP_WATER;
        float pitch = lava ? 0.85F : 1.15F;

        serverLevel.sendParticles(
                new BlockParticleOption(ParticleTypes.BLOCK, particleState),
                player.getX(),
                surfaceY + 0.04D,
                player.getZ(),
                8,
                player.getBbWidth() * 0.35D,
                0.02D,
                player.getBbWidth() * 0.35D,
                0.03D
        );
        serverLevel.playSound(null, player.getX(), surfaceY, player.getZ(), stepSound, SoundSource.PLAYERS, 0.45F, pitch);
    }

    private static void clearFluidRunVisuals(Player player) {
        if (!player.level().isClientSide()) {
            FLUID_RUN_VISUAL_STATE.remove(player.getUUID());
        }
    }

    // Builds or decays the flight ramp depending on whether DMZ's fast-flight aura should be active.
    private static void trackFlightMovement(ServerPlayer player) {
        UUID playerId = player.getUUID();
        if (isFastFlightAuraActive(player)) {
            int nextTicks = Math.min(DmzRevampConfig.REVAMP_SPEED_RAMP_TICKS.get(), FLIGHT_MOVEMENT_TICKS.getOrDefault(playerId, 0) + 1);
            FLIGHT_MOVEMENT_TICKS.put(playerId, nextTicks);
            return;
        }

        int currentTicks = FLIGHT_MOVEMENT_TICKS.getOrDefault(playerId, 0);
        if (currentTicks <= 0) {
            FLIGHT_MOVEMENT_TICKS.remove(playerId);
            return;
        }

        int nextTicks = Math.max(0, currentTicks - DmzRevampHelper.getRampDecayStep());
        if (nextTicks <= 0) {
            FLIGHT_MOVEMENT_TICKS.remove(playerId);
        } else {
            FLIGHT_MOVEMENT_TICKS.put(playerId, nextTicks);
        }
    }

    // Mirrors DMZ's fast-flight aura trigger closely enough for the server-side speed ramp.
    private static boolean isFastFlightAuraActive(ServerPlayer player) {
        StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).resolve().orElse(null);
        if (data == null || !data.getSkills().isSkillActive("fly") || !player.hasEffect(MainEffects.FLY.get())) {
            return false;
        }
        if (!player.isSprinting()) {
            return false;
        }
        int flightMode = data.getStatus().getFlightMode();
        if (flightMode == 1) {
            return true;
        }
        Vec3 movement = player.getDeltaMovement();
        return movement.lengthSqr() > SEARCH_FAST_FLIGHT_THRESHOLD_SQR;
    }

    // Returns the effective Speed bonus plus external movement modifiers that obey the revamp caps.
    private static double getRawMovementSpeedBonusPercent(Player player, StatsData data) {
        return sanitizePercent((DmzRevampHelper.getScaledMovementSpeedBonusPercent(data, DmzRevampHelper.getCurrentMovementSpeedValue(data))
                * DmzRevampConfig.REVAMP_MOVEMENT_SPEED_BONUS_MULTIPLIER.get())
                + getExternalMovementSpeedBonusPercent(player));
    }

    // Combines effective Speed and Ki Damage before flight caps are applied.
    private static double getRawFlightBonusPercent(Player player, StatsData data, boolean combatFlight) {
        double pairedSpeedBonus = DmzRevampHelper.getScaledPairedSpeedBonusPercent(data,
                DmzRevampHelper.getCurrentMovementSpeedValue(data), DmzRevampHelper.getCurrentMovementKiDamage(data));
        double bonus = (pairedSpeedBonus + getExternalMovementSpeedBonusPercent(player))
                * getFlightBonusMultiplier(combatFlight);
        return Math.max(0D, bonus);
    }

    private static double getOverhaulFlightBonusPercent(StatsData data, boolean combatFlight) {
        double pairedSpeedBonus = DmzRevampHelper.getScaledPairedSpeedBonusPercent(data,
                DmzRevampHelper.getCurrentMovementSpeedValue(data), DmzRevampHelper.getCurrentMovementKiDamage(data));
        return Math.max(0D, pairedSpeedBonus * getFlightBonusMultiplier(combatFlight));
    }

    private static double getRampedFlightBonusPercent(Player player, double allowedBonusPercent, int activeTicks, boolean canKeepRamp, boolean combatFlight) {
        double sanitizedAllowedBonus = Math.max(0D, allowedBonusPercent);
        if (!canKeepRamp || sanitizedAllowedBonus <= 0D) {
            return 0D;
        }

        double progress = Math.min(1D, Math.max(0, activeTicks) / (double) DmzRevampConfig.REVAMP_SPEED_RAMP_TICKS.get());
        double curvedProgress = progress * progress;
        double baseCap = Math.max(0D, getFlightBaseCapPercent(combatFlight) * DmzRevampHelper.getGravityMovementSpeedFactor(player));
        if (sanitizedAllowedBonus <= baseCap) {
            return sanitizedAllowedBonus;
        }
        return baseCap + ((sanitizedAllowedBonus - baseCap) * curvedProgress);
    }

    private static double getFlightSoftCappedBonusPercent(Player player, double rawBonusPercent, boolean combatFlight) {
        return combatFlight
                ? DmzRevampHelper.getFlightSoftCappedBonusPercent(player, rawBonusPercent)
                : DmzRevampHelper.getSearchFlightSoftCappedBonusPercent(player, rawBonusPercent);
    }

    private static double getFlightBonusMultiplier(boolean combatFlight) {
        return combatFlight
                ? DmzRevampConfig.REVAMP_FLIGHT_SPEED_BONUS_MULTIPLIER.get()
                : DmzRevampConfig.REVAMP_SEARCH_FLIGHT_SPEED_BONUS_MULTIPLIER.get();
    }

    private static double applyPlayerSpeedLimit(StatsData data, double bonusPercent) {
        if (data == null) return Math.max(0D, bonusPercent);
        int totalPercentLimit = ((SpeedLimitData) data).dmzrevamp$getSpeedLimit();
        return totalPercentLimit <= 0 ? Math.max(0D, bonusPercent)
                : Math.min(Math.max(0D, bonusPercent), Math.max(0D, totalPercentLimit - 100D));
    }

    private static boolean hasPlayerSpeedLimit(StatsData data) {
        return data != null && ((SpeedLimitData) data).dmzrevamp$getSpeedLimit() > 0;
    }

    private static double getFlightBaseCapPercent(boolean combatFlight) {
        return combatFlight
                ? DmzRevampConfig.REVAMP_FLIGHT_SPEED_BASE_CAP_PERCENT.get()
                : DmzRevampConfig.REVAMP_SEARCH_FLIGHT_SPEED_BASE_CAP_PERCENT.get();
    }

    // Returns the positive movement speed bonus added by external modifiers, excluding Overhaul and DMZ Sprint modifiers.
    private static double getExternalMovementSpeedBonusPercent(Player player) {
        AttributeInstance moveSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (moveSpeed == null) {
            return 0D;
        }
        return Math.max(0D, (getExternalMovementSpeedMultiplier(moveSpeed) - 1D) * 100D);
    }

    // Calculates the vanilla movement speed multiplier from external modifiers that should be folded into the SPD cap logic.
    private static double getExternalMovementSpeedMultiplier(AttributeInstance moveSpeed) {
        return getExternalAttributeMultiplier(moveSpeed, SPD_MOVE_SPEED_UUID, DMZ_SPRINT_SPEED_UUID, VANILLA_SPRINT_SPEED_UUID);
    }

    // Returns the positive swim speed bonus added by external modifiers, excluding the revamp swim modifier.
    private static double getExternalSwimSpeedBonusPercent(AttributeInstance swimSpeed) {
        if (swimSpeed == null) {
            return 0D;
        }
        return Math.max(0D, (getExternalSwimSpeedMultiplier(swimSpeed) - 1D) * 100D);
    }

    // Calculates the swim speed multiplier from external modifiers that should be folded into the SPD cap logic.
    private static double getExternalSwimSpeedMultiplier(AttributeInstance swimSpeed) {
        return getExternalAttributeMultiplier(swimSpeed, SPD_SWIM_SPEED_UUID);
    }

    // Calculates the final multiplier of an attribute while excluding modifiers owned by this mod or by exceptions.
    private static double getExternalAttributeMultiplier(AttributeInstance moveSpeed, UUID... ignoredModifierIds) {
        double base = moveSpeed.getBaseValue();
        if (base <= 0D || !Double.isFinite(base)) {
            return 1D;
        }

        double additive = 0D;
        double multiplyBase = 0D;
        double multiplyTotal = 1D;
        for (AttributeModifier modifier : moveSpeed.getModifiers()) {
            if (shouldIgnoreModifier(modifier, ignoredModifierIds)) {
                continue;
            }

            if (modifier.getOperation() == AttributeModifier.Operation.ADDITION) {
                additive += modifier.getAmount();
            } else if (modifier.getOperation() == AttributeModifier.Operation.MULTIPLY_BASE) {
                multiplyBase += modifier.getAmount();
            } else if (modifier.getOperation() == AttributeModifier.Operation.MULTIPLY_TOTAL) {
                multiplyTotal *= 1D + modifier.getAmount();
            }
        }

        double externalValue = (base + additive + (base * multiplyBase)) * multiplyTotal;
        if (!Double.isFinite(externalValue) || externalValue <= 0D) {
            return 1D;
        }
        return Math.max(0.01D, externalValue / base);
    }

    // Returns true when a modifier should not be folded into the capped SPD calculation.
    private static boolean shouldIgnoreModifier(AttributeModifier modifier, UUID... ignoredModifierIds) {
        UUID id = modifier.getId();
        for (UUID ignoredModifierId : ignoredModifierIds) {
            if (ignoredModifierId.equals(id)) {
                return true;
            }
        }
        return false;
    }

    private static final class SpeedState {
        private double moveSpeedBonus = Double.NaN;
        private double effectiveMoveSpeedBonusPercent = Double.NaN;
        private double attackSpeedBonus = Double.NaN;
        private double swimSpeedBonus = Double.NaN;
        private double stepHeightAddition = Double.NaN;
        private int speedRampTicks;
    }

    private static final class FluidRunVisualState {
        private double lastX;
        private double lastZ;
        private double stepDistance;

        private FluidRunVisualState(double lastX, double lastZ) {
            this.lastX = lastX;
            this.lastZ = lastZ;
        }
    }
}
