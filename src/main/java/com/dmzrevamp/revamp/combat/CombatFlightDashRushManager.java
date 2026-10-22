package com.dmzrevamp.revamp.combat;

import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.network.S2C.StrikeRushS2C;
import com.dragonminez.common.combat.logic.player.TargetHelper;
import com.dragonminez.common.init.entities.MastersEntity;
import com.dragonminez.common.init.EntityAttributes;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.server.events.players.combat.StrikeAttackHandler;
import com.dmzrevamp.DmzRevampMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.InteractionHand;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-side controller for the special Combat Flight Double Dash Strike Rush. */
@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CombatFlightDashRushManager {
    private static final double MAX_LOCK_RANGE = 64.0D;
    private static final double CONNECT_GAP = 1.8D;
    private static final int RUSH_TICKS = 40;
    private static final String KB_MULT_TAG = "dmzrevamp_dd_rush_knockback";
    private static final String KB_TICK_TAG = "dmzrevamp_dd_rush_knockback_tick";
    private static final Map<UUID, Integer> LOCK_TARGETS = new HashMap<>();
    private static final Map<UUID, Rush> RUSHES = new HashMap<>();
    private static final Map<UUID, UUID> DAMAGE_BOOSTS = new HashMap<>();

    private CombatFlightDashRushManager() {}

    public static void updateLockTarget(ServerPlayer player, int targetId) {
        if (targetId < 0) {
            LOCK_TARGETS.remove(player.getUUID());
            return;
        }
        Entity candidate = player.level().getEntity(targetId);
        if (!(candidate instanceof LivingEntity target) || !isValidTarget(player, target)) {
            LOCK_TARGETS.remove(player.getUUID());
            return;
        }
        LOCK_TARGETS.put(player.getUUID(), targetId);
    }

    public static LivingEntity getValidLockTarget(ServerPlayer player) {
        Integer id = LOCK_TARGETS.get(player.getUUID());
        if (id == null) return null;
        Entity entity = player.level().getEntity(id);
        if (!(entity instanceof LivingEntity target) || !isValidTarget(player, target)) {
            LOCK_TARGETS.remove(player.getUUID());
            return null;
        }
        return target;
    }

    private static boolean isValidTarget(ServerPlayer player, LivingEntity target) {
        return target != player && target.isAlive() && target.level() == player.level()
                && player.distanceToSqr(target) <= MAX_LOCK_RANGE * MAX_LOCK_RANGE
                && !target.isInvisibleTo(player)
                && !(target instanceof MastersEntity)
                && TargetHelper.getRelation(player, target) != TargetHelper.Relation.FRIENDLY;
    }

    public static void start(ServerPlayer player, LivingEntity target) {
        if (player == null || target == null || !isValidTarget(player, target)) return;
        RUSHES.put(player.getUUID(), new Rush(target.getUUID(), RUSH_TICKS));
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
            stats.getStatus().setAuraActive(true);
            NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
        });
        NetworkHandler.sendToTrackingEntityAndSelf(new StrikeRushS2C(player.getId(), RUSH_TICKS, target.getId(), false), player);
    }

    public static boolean isActive(ServerPlayer player) {
        return player != null && RUSHES.containsKey(player.getUUID());
    }

    /** If a regular Strike Rush connects first, suppress this dash's basic-hit branch. */
    public static void cancelForNormalStrike(ServerPlayer dashPlayer) {
        if (dashPlayer != null && RUSHES.containsKey(dashPlayer.getUUID())) finish(dashPlayer, true);
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        Rush rush = RUSHES.get(player.getUUID());
        if (rush == null) return;
        if (!player.isAlive() || player.isRemoved() || player.isPassenger() || --rush.ticks <= 0) {
            finish(player, false);
            return;
        }
        Entity entity = player.serverLevel().getEntity(rush.targetId);
        if (!(entity instanceof LivingEntity target) || !isValidTarget(player, target)) {
            finish(player, false);
            return;
        }

        if (target instanceof ServerPlayer other && StrikeAttackHandler.isRushing(other)) {
            // Native Strike Rush wins the mixed collision; let its normal strike resolve.
            finish(player, true);
            return;
        }

        double gap = distanceToBox(player.getBoundingBox().getCenter(), target.getBoundingBox());
        if (gap <= CONNECT_GAP) {
            if (target instanceof ServerPlayer other && RUSHES.containsKey(other.getUUID())) {
                // Two dash Rushes produce simultaneous ordinary hits, never a Strike Clash.
                RUSHES.remove(player.getUUID());
                RUSHES.remove(other.getUUID());
                basicRushHit(player, other);
                basicRushHit(other, player);
                finishVisuals(player, true);
                finishVisuals(other, true);
                return;
            }
            basicRushHit(player, target);
            finish(player, true);
            return;
        }

        Vec3 direction = target.getBoundingBox().getCenter().subtract(player.getBoundingBox().getCenter()).normalize();
        double speed = Math.min(getRushSpeed(player), Math.max(0.25D, gap - CONNECT_GAP));
        player.setDeltaMovement(direction.scale(speed));
        player.fallDistance = 0F;
        player.hasImpulse = true;
    }

    private static double distanceToBox(Vec3 point, AABB box) {
        double x = Math.max(box.minX, Math.min(point.x, box.maxX));
        double y = Math.max(box.minY, Math.min(point.y, box.maxY));
        double z = Math.max(box.minZ, Math.min(point.z, box.maxZ));
        return point.distanceTo(new Vec3(x, y, z));
    }

    private static double getRushSpeed(ServerPlayer player) {
        return StatsProvider.get(StatsCapability.INSTANCE, player).map(stats -> {
            int flyLevel = stats.getSkills().getSkillLevel("fly");
            double flyAttribute = player.getAttribute(EntityAttributes.FLY_SPEED.get()) == null
                    ? 0.0D : player.getAttributeValue(EntityAttributes.FLY_SPEED.get());
            double scale = flyAttribute <= 0.0D ? 1.0D : Mth.clamp(flyAttribute / 0.35D, 0.25D, 4.0D);
            scale *= stats.getFlightStateSpeedMultiplier();
            return Mth.clamp(1.05D * (1.0D + 0.20D * flyLevel) * scale * 0.6D, 0.9D, 1.5D);
        }).orElse(0.9D);
    }

    private static void basicRushHit(ServerPlayer attacker, LivingEntity target) {
        if (!attacker.isAlive() || !target.isAlive()) return;
        attacker.swing(InteractionHand.MAIN_HAND, true);
        DAMAGE_BOOSTS.put(attacker.getUUID(), target.getUUID());
        target.getPersistentData().putDouble(KB_MULT_TAG, 1.5D);
        target.getPersistentData().putLong(KB_TICK_TAG, target.level().getGameTime());
        try {
            attacker.attack(target);
        } finally {
            DAMAGE_BOOSTS.remove(attacker.getUUID());
            if (target.getPersistentData().getLong(KB_TICK_TAG) == target.level().getGameTime()) {
                target.getPersistentData().remove(KB_MULT_TAG);
                target.getPersistentData().remove(KB_TICK_TAG);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void increaseRushBasicDamage(DMZEvent.DamageModifyEvent event) {
        if (event.getSourceType() != DMZEvent.DamageSourceType.MELEE || !(event.getAttacker() instanceof ServerPlayer attacker)) return;
        UUID expectedTarget = DAMAGE_BOOSTS.get(attacker.getUUID());
        if (expectedTarget == null || !expectedTarget.equals(event.getVictim().getUUID())) return;
        event.setAmount(event.getAmount() * 1.10D);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void increaseRushBasicKnockback(LivingKnockBackEvent event) {
        LivingEntity target = event.getEntity();
        if (!target.getPersistentData().contains(KB_MULT_TAG)
                || target.getPersistentData().getLong(KB_TICK_TAG) != target.level().getGameTime()) return;
        event.setStrength((float) (event.getStrength() * target.getPersistentData().getDouble(KB_MULT_TAG)));
        target.getPersistentData().remove(KB_MULT_TAG);
        target.getPersistentData().remove(KB_TICK_TAG);
    }

    private static void finish(ServerPlayer player, boolean connected) {
        RUSHES.remove(player.getUUID());
        finishVisuals(player, connected);
    }

    private static void finishVisuals(ServerPlayer player, boolean connected) {
        NetworkHandler.sendToTrackingEntityAndSelf(new StrikeRushS2C(player.getId(), 0, -1, connected), player);
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
            stats.getStatus().setAuraActive(false);
            NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
        });
        player.setDeltaMovement(connected ? Vec3.ZERO : player.getDeltaMovement().scale(0.35D));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        LOCK_TARGETS.remove(id);
        RUSHES.remove(id);
        DAMAGE_BOOSTS.remove(id);
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        LOCK_TARGETS.remove(event.getEntity().getUUID());
        if (event.getEntity() instanceof ServerPlayer player) finish(player, false);
    }

    private static final class Rush {
        private final UUID targetId;
        private int ticks;

        private Rush(UUID targetId, int ticks) {
            this.targetId = targetId;
            this.ticks = ticks;
        }
    }
}
