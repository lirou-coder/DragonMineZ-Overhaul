package com.dmzrevamp.revamp.strike;

import com.dmzrevamp.config.StrikeClashConfigured;
import com.dmzrevamp.mixin.StrikeAttackHandlerStateAccessor;
import com.dmzrevamp.mixin.StrikeAttackActiveAccessor;
import com.dmzrevamp.mixin.StrikeAttackPendingAccessor;
import com.dmzrevamp.mixin.DBSagasEntityComboTargetAccessor;
import com.dragonminez.common.combat.clash.StrikeClash;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.dragonminez.server.events.players.combat.StrikeAttackHandler;
import com.dmzrevamp.network.DmzRevampNetwork;
import com.dmzrevamp.network.StrikeClashModeS2CPacket;
import com.dmzrevamp.revamp.battlepower.AccurateMobBattlePowerCalculator;
import com.dragonminez.common.combat.clash.ClashParticipant;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.server.events.players.combat.StrikeAttackHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.PacketDistributor;

import java.util.Map;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.reflect.Method;

/** Small compatibility layer for DMZ 2.2's native Strike Clash. */
public final class StrikeClashManager {
    private static final java.util.Set<String> NON_CLASH_STRIKES = java.util.Set.of(
            "oozaru_slam", "dimensional_punch", "dimensional_sword_attack",
            "blue_hurricane", StrikeAttackTemplates.NAMEKIAN_REGENERATION
    );
    private static final Map<Long, Float> NATIVE_STRIKE_AREAS = new ConcurrentHashMap<>();
    private static final Map<java.util.UUID, ClashParticipant> NATIVE_PARTICIPANTS = new ConcurrentHashMap<>();
    private static final Map<String, Field> NATIVE_STATE_FIELDS = new ConcurrentHashMap<>();
    private static final Map<java.util.UUID, Long> LIVING_WORLD_GRACE_WINDOWS = new ConcurrentHashMap<>();
    private static volatile Method LIVING_WORLD_TRY_START;
    private static volatile Method LIVING_WORLD_IS_COMBOING;

    private StrikeClashManager() {}

    /** Starts the same late-input retry window for Living World rushes that DMZ rushes get. */
    public static void scheduleLivingWorldGrace(LivingEntity fighter) {
        if (!ModList.get().isLoaded("dmzlivingworld") || !StrikeClashConfigured.get().enabled
                || fighter == null || fighter.level().isClientSide) return;
        int window = StrikeClashConfigured.get().strikeClashLateInputWindowTicks;
        if (window <= 0) return;
        LIVING_WORLD_GRACE_WINDOWS.put(fighter.getUUID(), fighter.level().getGameTime() + window);
    }

    /** Retry Living World's one-shot clash discovery until its configured grace window expires. */
    public static void tickLivingWorldGrace(LivingEntity fighter) {
        if (!(fighter.level() instanceof ServerLevel) || !ModList.get().isLoaded("dmzlivingworld")) return;
        Long expiresAt = LIVING_WORLD_GRACE_WINDOWS.get(fighter.getUUID());
        if (expiresAt == null) return;
        if (!StrikeClashConfigured.get().enabled || !fighter.isAlive()
                || com.dragonminez.common.combat.clash.BeamClashManager.isClashing(fighter.getUUID())) {
            LIVING_WORLD_GRACE_WINDOWS.remove(fighter.getUUID());
            return;
        }
        long now = fighter.level().getGameTime();
        if (isLivingWorldComboing(fighter)) {
            LIVING_WORLD_GRACE_WINDOWS.put(fighter.getUUID(), now + StrikeClashConfigured.get().strikeClashLateInputWindowTicks);
            expiresAt = now + StrikeClashConfigured.get().strikeClashLateInputWindowTicks;
        } else if (now > expiresAt) {
            LIVING_WORLD_GRACE_WINDOWS.remove(fighter.getUUID());
            return;
        }
        try {
            Method method = livingWorldTryStartMethod();
            if (method != null) method.invoke(null, fighter);
            if (com.dragonminez.common.combat.clash.BeamClashManager.isClashing(fighter.getUUID())) {
                LIVING_WORLD_GRACE_WINDOWS.remove(fighter.getUUID());
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            LIVING_WORLD_GRACE_WINDOWS.remove(fighter.getUUID());
        }
    }

    /** Eligibility used inside Living World's own rushing predicate, including its grace tail. */
    public static boolean isLivingWorldRushing(Object fighter) {
        if (!(fighter instanceof LivingEntity entity) || entity.level().isClientSide
                || !StrikeClashConfigured.get().enabled || !entity.isAlive()
                || com.dragonminez.common.combat.clash.BeamClashManager.isClashing(entity.getUUID())) return false;
        Long expiresAt = LIVING_WORLD_GRACE_WINDOWS.get(entity.getUUID());
        return expiresAt != null && (isLivingWorldComboing(fighter) || entity.level().getGameTime() <= expiresAt);
    }

    private static boolean isLivingWorldComboing(Object fighter) {
        try {
            Method method = LIVING_WORLD_IS_COMBOING;
            if (method == null) {
                method = fighter.getClass().getMethod("isComboing");
                LIVING_WORLD_IS_COMBOING = method;
            }
            return Boolean.TRUE.equals(method.invoke(fighter));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    private static Method livingWorldTryStartMethod() {
        Method method = LIVING_WORLD_TRY_START;
        if (method != null) return method;
        try {
            Class<?> compat = Class.forName("com.dmzlivingworld.world.StrikeClashCompat");
            method = compat.getMethod("tryStart", LivingEntity.class);
            method.setAccessible(true);
            LIVING_WORLD_TRY_START = method;
            return method;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    /** Kept as the single Overhaul policy used by the rush gate and activation delay. */
    public static boolean canTechniqueStartClash(String techniqueId) {
        return techniqueId != null && !techniqueId.isBlank() && !NON_CLASH_STRIKES.contains(techniqueId);
    }

    public static boolean canPlayerStartNativeClash(ServerPlayer player) {
        if (!StrikeClashConfigured.get().enabled || player == null) return false;
        Object pending = StrikeAttackHandlerStateAccessor.dmzrevamp$getPendingStrikes().get(player.getUUID());
        if (pending instanceof StrikeAttackPendingAccessor access
                && canTechniqueStartClash(access.dmzrevamp$getTechniqueId())) return true;

        int graceTicks = StrikeClashConfigured.get().strikeClashLateInputWindowTicks;
        if (graceTicks <= 0) return false;
        Object active = StrikeAttackHandlerStateAccessor.dmzrevamp$getActiveStrikes().get(player.getUUID());
        return active instanceof StrikeAttackActiveAccessor access
                && access.dmzrevamp$getTicksElapsed() <= graceTicks
                && canTechniqueStartClash(access.dmzrevamp$getTechniqueId());
    }

    /** NPC combos use DMZ's native rush state only for a few combo types; admit other targeted combos too. */
    public static boolean canNpcStartNativeClash(DBSagasEntity npc) {
        if (!StrikeClashConfigured.get().enabled || npc == null || !npc.isAlive()
                || !npc.isComboing() || npc.isStrikeClashing()) return false;
        DBSagasEntity.ComboType combo = DBSagasEntity.ComboType.fromId(npc.getComboId());
        if (combo == null || combo == DBSagasEntity.ComboType.SLEEP_RECOVERY) return false;
        LivingEntity target = ((DBSagasEntityComboTargetAccessor) npc).dmzrevamp$getComboTarget();
        return target != null && target.isAlive() && target.level() == npc.level();
    }

    public static java.util.UUID nativePlayerStrikeTarget(ServerPlayer player) {
        if (player == null) return null;
        Object pending = StrikeAttackHandlerStateAccessor.dmzrevamp$getPendingStrikes().get(player.getUUID());
        if (pending instanceof StrikeAttackPendingAccessor access) {
            return access.dmzrevamp$getPreferredTargetId();
        }
        Object active = StrikeAttackHandlerStateAccessor.dmzrevamp$getActiveStrikes().get(player.getUUID());
        return active instanceof StrikeAttackActiveAccessor access ? access.dmzrevamp$getTargetId() : null;
    }

    /** Called by DMZ's own StrikeClash.start after the participant has been accepted. */
    public static StrikeAttackHandler.ClashRush takePlayerRushForNativeClash(ServerPlayer player) {
        StrikeAttackHandler.ClashRush rush = StrikeAttackHandler.takeRushForClash(player);
        if (rush != null || player == null) return rush;

        Object active = StrikeAttackHandlerStateAccessor.dmzrevamp$getActiveStrikes().get(player.getUUID());
        if (StrikeClashConfigured.get().strikeClashLateInputWindowTicks <= 0
                || !(active instanceof StrikeAttackActiveAccessor access)
                || access.dmzrevamp$getTicksElapsed() > StrikeClashConfigured.get().strikeClashLateInputWindowTicks
                || !canTechniqueStartClash(access.dmzrevamp$getTechniqueId())) return null;

        StrikeAttackHandler.ClashRush activeRush = new StrikeAttackHandler.ClashRush(
                access.dmzrevamp$getTechniqueId(), access.dmzrevamp$getCooldownTicks());
        // A launched strike must not keep ticking or dealing hits while its owner is in the clash.
        StrikeAttackHandler.interrupt(player);
        return activeRush;
    }

    /** Called for the DMZ StrikeClash participant constructor (not Ki beam participants). */
    public static void registerNativeParticipant(ClashParticipant participant) {
        if (participant == null || participant.beam() != null) return;
        NATIVE_STRIKE_AREAS.put(participant.meterSeed(), 1.0F);
        NATIVE_PARTICIPANTS.put(participant.owner().getUUID(), participant);
    }

    public static void registerNativeClash(ClashParticipant first, ClashParticipant second) {
        if (first == null || second == null) return;
        NATIVE_STRIKE_AREAS.put(first.meterSeed(), goodAreaMultiplier(first.owner(), second.owner()));
        NATIVE_STRIKE_AREAS.put(second.meterSeed(), goodAreaMultiplier(second.owner(), first.owner()));
    }

    public static void unregisterNativeParticipant(ClashParticipant participant) {
        if (participant != null) {
            NATIVE_STRIKE_AREAS.remove(participant.meterSeed());
            NATIVE_PARTICIPANTS.remove(participant.owner().getUUID(), participant);
        }
    }

    public static void beginNativeClash(LivingEntity first, LivingEntity second) {
        ClashParticipant firstParticipant = NATIVE_PARTICIPANTS.get(first.getUUID());
        ClashParticipant secondParticipant = NATIVE_PARTICIPANTS.get(second.getUUID());
        registerNativeClash(firstParticipant, secondParticipant);
        sendNativeMode(first, goodAreaMultiplier(first, second), true);
        sendNativeMode(second, goodAreaMultiplier(second, first), true);
    }

    public static void refreshNativeMeleePower(ClashParticipant participant) {
        if (participant == null || !(participant.owner() instanceof ServerPlayer player)) return;
        double melee = StatsProvider.get(StatsCapability.INSTANCE, player)
                .map(data -> data.getMeleeDamage()).orElse(1D);
        double power = configuredMeleePower(melee);
        if (participant instanceof ClashParticipantPowerAccess access) {
            access.dmzrevamp$setStatPower(Double.isFinite(power) ? Math.max(1D, power) : 1D);
        }
    }

    /** Reads the private Side records without mixin accessors whose descriptors vary by DMZ build. */
    public static void refreshNativeClashState(Object clash) {
        Object first = readNativeStateField(clash, "a");
        Object second = readNativeStateField(clash, "b");
        refreshNativeSide(first);
        refreshNativeSide(second);

        Object firstEntity = readNativeStateField(first, "entity");
        Object secondEntity = readNativeStateField(second, "entity");
        if (firstEntity instanceof LivingEntity a && secondEntity instanceof LivingEntity b) {
            beginNativeClash(a, b);
        }
    }

    public static void endNativeClashSide(Object side) {
        Object participant = readNativeStateField(side, "participant");
        if (participant instanceof ClashParticipant clashParticipant) endNativeClash(clashParticipant);
    }

    private static void refreshNativeSide(Object side) {
        Object participant = readNativeStateField(side, "participant");
        if (participant instanceof ClashParticipant clashParticipant) refreshNativeMeleePower(clashParticipant);
    }

    private static Object readNativeStateField(Object target, String name) {
        if (target == null) return null;
        String key = target.getClass().getName() + '#' + name;
        try {
            Field field = NATIVE_STATE_FIELDS.computeIfAbsent(key, ignored -> {
                try {
                    Field found = target.getClass().getDeclaredField(name);
                    found.setAccessible(true);
                    return found;
                } catch (ReflectiveOperationException exception) {
                    throw new IllegalStateException(exception);
                }
            });
            return field.get(target);
        } catch (ReflectiveOperationException | IllegalStateException exception) {
            return null;
        }
    }

    public static double configuredMeleePower(double melee) {
        var config = StrikeClashConfigured.get();
        return config.meleeDMGInfluence
                ? 1D + (Math.max(0D, melee) - 1D) * config.meleeDMGInfluenceMultiplier
                : 1D;
    }

    public static void endNativeClash(ClashParticipant participant) {
        unregisterNativeParticipant(participant);
        if (participant != null) sendNativeMode(participant.owner(), 1F, false);
    }

    private static void sendNativeMode(LivingEntity entity, float areaMultiplier, boolean active) {
        StrikeClashConfigured.Config config = StrikeClashConfigured.get();
        DmzRevampNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity),
                new StrikeClashModeS2CPacket(entity.getId(), active, areaMultiplier,
                        config.meterSpeedMultiplier, config.goodAreaSizeMultiplier,
                        config.perfectAreaFraction, config.goodMinimumEfficiency));
    }

    public static Float nativeStrikeArea(long meterSeed) {
        return NATIVE_STRIKE_AREAS.get(meterSeed);
    }

    /** Area advantage is relative: player-v-player uses DMZ Speed; NPC-v-NPC uses custom BP. */
    public static float goodAreaMultiplier(LivingEntity entity, LivingEntity opponent) {
        StrikeClashConfigured.Config config = StrikeClashConfigured.get();
        if (!config.goodAreaSpeedInfluence || entity == null || opponent == null) return 1.0F;
        double own;
        double other;
        if (entity instanceof ServerPlayer first && opponent instanceof ServerPlayer second) {
            own = StatsProvider.get(StatsCapability.INSTANCE, first).map(data -> data.getSpeed()).orElse(0D);
            other = StatsProvider.get(StatsCapability.INSTANCE, second).map(data -> data.getSpeed()).orElse(0D);
        } else if (!(entity instanceof ServerPlayer) && !(opponent instanceof ServerPlayer)) {
            own = AccurateMobBattlePowerCalculator.calculateCurvedBattlePowerExact(entity);
            other = AccurateMobBattlePowerCalculator.calculateCurvedBattlePowerExact(opponent);
        } else {
            return 1.0F;
        }
        if (!Double.isFinite(own) || !Double.isFinite(other) || own <= other || other <= 0D) return 1.0F;
        double multiplier = 1D + ((own / other) - 1D) * config.goodAreaSpeedInfluenceMultiplier;
        return (float) Math.max(1D, Math.min(Float.MAX_VALUE, multiplier));
    }
}
