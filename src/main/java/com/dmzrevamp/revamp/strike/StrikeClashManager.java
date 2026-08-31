package com.dmzrevamp.revamp.strike;

import com.dmzrevamp.DmzRevampMod;
import com.dmzrevamp.config.StrikeClashConfigured;
import com.dmzrevamp.mixin.StrikeAttackActiveAccessor;
import com.dmzrevamp.mixin.StrikeAttackHandlerStateAccessor;
import com.dmzrevamp.network.DmzRevampNetwork;
import com.dmzrevamp.network.StrikeClashModeS2CPacket;
import com.dragonminez.common.combat.clash.BeamClashManager;
import com.dragonminez.common.combat.logic.player.PlayerAttackHelper;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.BeamClashStateS2C;
import com.dragonminez.common.network.S2C.MeleeAnimationS2C;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.network.S2C.TriggerAnimationS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Server-authoritative two-participant Strike/Combo clash. */
@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class StrikeClashManager {
    private static final float DRIFT_PER_TICK = 0.005F;
    private static final float POWER_FLOOR = 0.6F;
    private static final float POWER_SPAN = 0.95F;
    private static final float MIN_TRACTION = 0.35F;
    private static final float WIN_THRESHOLD = 0.8F;
    private static final float BURST_PER_PRESS = 0.6F;
    private static final List<Clash> ACTIVE = new ArrayList<>();
    private static final Map<UUID, Float> WINNER_DAMAGE_BOOST = new HashMap<>();
    private static final Map<UUID, LivingEntity> WINNER_DAMAGE_ENTITY = new HashMap<>();

    @SuppressWarnings("unchecked")
    private static final RegistryObject<SoundEvent>[] PUNCH_SOUNDS = new RegistryObject[]{
            MainSounds.GOLPE1, MainSounds.GOLPE2, MainSounds.GOLPE3,
            MainSounds.GOLPE4, MainSounds.GOLPE5, MainSounds.GOLPE6
    };

    private StrikeClashManager() {
    }

    /** Called on the first hit of a newly-created player Strike, before damage is applied. */
    public static boolean tryStart(ServerPlayer attacker, LivingEntity target) {
        if (!StrikeClashConfigured.get().enabled || attacker == null || target == null
                || !attacker.isAlive() || !target.isAlive()
                || isClashing(attacker.getUUID()) || isClashing(target.getUUID())
                || BeamClashManager.isClashing(attacker.getUUID())
                || BeamClashManager.isClashing(target.getUUID())) {
            return false;
        }

        Object attackerStrike = activeStrikes().get(attacker.getUUID());
        if (!(attackerStrike instanceof StrikeAttackActiveAccessor attackerActive)) {
            return false;
        }

        Object opponentStrike = null;
        if (target instanceof ServerPlayer targetPlayer) {
            opponentStrike = activeStrikes().get(targetPlayer.getUUID());
            if (!(opponentStrike instanceof StrikeAttackActiveAccessor targetActive)
                    || !attacker.getUUID().equals(targetActive.dmzrevamp$getTargetId())) {
                return false;
            }
        } else if (!(target instanceof DBSagasEntity saga) || !saga.isComboing()) {
            return false;
        }

        Clash clash = new Clash(
                new Participant(attacker, attackerStrike),
                new Participant(target, opponentStrike)
        );
        abortPlayerStrike(clash.a);
        abortPlayerStrike(clash.b);
        clash.alignAndLock();
        ACTIVE.add(clash);
        sendMode(clash.a.entity, true);
        sendMode(clash.b.entity, true);
        stopPlayerTechniqueAnimation(clash.a.entity);
        stopPlayerTechniqueAnimation(clash.b.entity);
        return true;
    }

    public static boolean isClashing(UUID entityId) {
        if (entityId == null) return false;
        for (Clash clash : ACTIVE) {
            if (clash.involves(entityId)) return true;
        }
        return false;
    }

    /** Pauses native ActiveStrike timelines until this manager resolves the clash. */
    public static boolean shouldPauseStrike(ServerPlayer player) {
        return player != null && isClashing(player.getUUID());
    }

    /** startStrike continues after its first damage hook; clean up the lock/animation it reapplies. */
    public static void finalizePlayerStrikeAbort(ServerPlayer player) {
        if (player == null) return;
        for (Clash clash : ACTIVE) {
            Participant participant = clash.participant(player.getUUID());
            if (participant != null) {
                abortPlayerStrike(clash.a);
                abortPlayerStrike(clash.b);
                return;
            }
        }
    }

    /** Routes DMZ's existing clash input packet to this clash before BeamClash handles it. */
    public static boolean handlePlayerPress(ServerPlayer player) {
        if (player == null) return false;
        for (Clash clash : ACTIVE) {
            Participant participant = clash.participant(player.getUUID());
            if (participant != null) {
                participant.press(clash.other(participant));
                playPunch(player);
                return true;
            }
        }
        return false;
    }

    public static float scaleWinningPlayerDamage(ServerPlayer attacker, float damage) {
        if (attacker == null || damage <= 0F) return damage;
        Float multiplier = WINNER_DAMAGE_BOOST.get(attacker.getUUID());
        return multiplier == null ? damage : damage * multiplier;
    }

    public static double scaleWinningPlayerDamage(ServerPlayer attacker, double damage) {
        if (attacker == null || damage <= 0D) return damage;
        Float multiplier = WINNER_DAMAGE_BOOST.get(attacker.getUUID());
        return multiplier == null ? damage : damage * multiplier;
    }

    @SubscribeEvent
    public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;

        Iterator<Clash> iterator = ACTIVE.iterator();
        while (iterator.hasNext()) {
            Clash clash = iterator.next();
            Result result = clash.tick(level);
            if (result == Result.ONGOING) continue;
            clash.finish(result);
            iterator.remove();
        }
        cleanupDamageBoosts();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void protectParticipants(LivingAttackEvent event) {
        if (!event.getEntity().level().isClientSide() && isClashing(event.getEntity().getUUID())) {
            event.setCanceled(true);
        }
    }

    /** NPC combo damage receives the winner multiplier while that original combo continues. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void boostWinningNpcCombo(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof DBSagasEntity saga) || !saga.isComboing()) return;
        Float multiplier = WINNER_DAMAGE_BOOST.get(saga.getUUID());
        if (multiplier != null && event.getAmount() > 0F) {
            event.setAmount(event.getAmount() * multiplier);
        }
    }

    private static void cleanupDamageBoosts() {
        WINNER_DAMAGE_BOOST.entrySet().removeIf(entry -> {
            UUID id = entry.getKey();
            if (activeStrikes().containsKey(id)) return false;
            LivingEntity entity = WINNER_DAMAGE_ENTITY.get(id);
            if (entity instanceof DBSagasEntity saga && saga.isAlive() && saga.isComboing()) return false;
            for (Clash clash : ACTIVE) if (clash.involves(id)) return false;
            WINNER_DAMAGE_ENTITY.remove(id);
            return true;
        });
    }

    private static Map<UUID, Object> activeStrikes() {
        return StrikeAttackHandlerStateAccessor.dmzrevamp$getActiveStrikes();
    }

    private static void sendMode(LivingEntity entity, boolean active) {
        DmzRevampNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity),
                new StrikeClashModeS2CPacket(entity.getId(), active));
    }

    private static void stopPlayerTechniqueAnimation(LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) return;
        NetworkHandler.sendToTrackingEntityAndSelf(new TriggerAnimationS2C(
                player.getUUID(), TriggerAnimationS2C.AnimationType.KI_ANIMATION_STOP,
                0, -1, ""), player);
    }

    private static void sendState(Clash clash, Participant participant) {
        if (!(participant.entity instanceof ServerPlayer player)) return;
        Participant opponent = clash.other(participant);
        float[] goodArea = participant.goodArea(opponent);
        NetworkHandler.sendToPlayer(new BeamClashStateS2C(
                true,
                participant.meterPhase,
                goodArea[0],
                goodArea[1],
                clash.advantageFor(participant),
                auraColor(participant.entity),
                opponent.entity.getId()
        ), player);
    }

    private static void notifyEnded(LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            NetworkHandler.sendToPlayer(BeamClashStateS2C.inactive(), player);
            sendMode(player, false);
        }
    }

    private static void playPunch(LivingEntity entity) {
        SoundEvent sound = PUNCH_SOUNDS[entity.getRandom().nextInt(PUNCH_SOUNDS.length)].get();
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), sound,
                SoundSource.PLAYERS, 1F, 0.9F + entity.getRandom().nextFloat() * 0.2F);
    }

    private static int auraColor(LivingEntity entity) {
        if (entity instanceof DBSagasEntity saga) return saga.getAuraColor();
        if (!(entity instanceof ServerPlayer player)) return 0xFFFFFF;
        StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).resolve().orElse(null);
        if (data == null) return 0xFFFFFF;
        float[] rgb = data.getCharacter().getRgbAuraColor();
        if (data.getCharacter().hasActiveForm() && data.getCharacter().getActiveFormData() != null
                && data.getCharacter().getActiveFormData().getRgbAuraColor() != null) {
            rgb = data.getCharacter().getActiveFormData().getRgbAuraColor();
        }
        if (data.getCharacter().hasActiveStackForm() && data.getCharacter().getActiveStackFormData() != null
                && data.getCharacter().getActiveStackFormData().getRgbAuraColor() != null) {
            rgb = data.getCharacter().getActiveStackFormData().getRgbAuraColor();
        }
        if (rgb == null || rgb.length < 3) return 0xFFFFFF;
        int r = Mth.clamp(Math.round(rgb[0] * 255F), 0, 255);
        int g = Mth.clamp(Math.round(rgb[1] * 255F), 0, 255);
        int b = Mth.clamp(Math.round(rgb[2] * 255F), 0, 255);
        return (r << 16) | (g << 8) | b;
    }

    private static double meleeDamage(LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).resolve().orElse(null);
            if (data != null) return Math.max(1D, data.getMeleeDamage());
        }
        AttributeInstance attack = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        return Math.max(1D, attack == null ? 1D : attack.getValue());
    }

    private static double speedFor(Participant participant) {
        if (participant.entity instanceof ServerPlayer player) {
            StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).resolve().orElse(null);
            if (data != null) return Math.max(0.0001D, data.getStrikeDamage());
        }
        AttributeInstance attackDamage = participant.entity.getAttribute(Attributes.ATTACK_DAMAGE);
        return Math.max(0.0001D, attackDamage == null ? 1D : attackDamage.getValue());
    }

    private static int visualAttackInterval(LivingEntity entity) {
        AttributeInstance attackSpeed = entity.getAttribute(Attributes.ATTACK_SPEED);
        double value = attackSpeed != null && attackSpeed.getValue() > 0D ? attackSpeed.getValue() : 4D;
        return Math.max(1, (int) Math.ceil(20D / value));
    }

    private static void face(LivingEntity source, LivingEntity target) {
        source.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
        source.setYHeadRot(source.getYRot());
        source.setYBodyRot(source.getYRot());
    }

    private static void lockAt(LivingEntity entity, Vec3 position) {
        if (entity instanceof ServerPlayer player) player.teleportTo(position.x, position.y, position.z);
        else entity.setPos(position.x, position.y, position.z);
        entity.setDeltaMovement(Vec3.ZERO);
        entity.hurtMarked = true;
        entity.fallDistance = 0F;
    }

    private static void stopPlayerStrike(Participant participant) {
        if (!(participant.entity instanceof ServerPlayer player)) return;
        Object removed = activeStrikes().remove(player.getUUID());
        Object captured = removed != null ? removed : participant.strike;
        StrikeAttackActiveAccessor accessor = captured instanceof StrikeAttackActiveAccessor value ? value : null;
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
                data.getStatus().setStrikeLocked(false);
            if (accessor != null) {
                data.getCooldowns().setCooldown("TechniqueCooldown_" + accessor.dmzrevamp$getTechniqueId(),
                        accessor.dmzrevamp$getCooldownTicks());
            }
            NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
        });
        FlyingStrikeYLock.finish(player);
        NetworkHandler.sendToTrackingEntityAndSelf(new TriggerAnimationS2C(
                player.getUUID(), TriggerAnimationS2C.AnimationType.KI_ANIMATION_STOP, 0, -1, ""), player);
    }

    private static void abortPlayerStrike(Participant participant) {
        if (!(participant.entity instanceof ServerPlayer player)) return;
        activeStrikes().remove(player.getUUID());
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            data.getStatus().setStrikeLocked(false);
            NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
        });
        FlyingStrikeYLock.finish(player);
        stopPlayerTechniqueAnimation(player);
    }

    private static void restartWinningPlayerStrike(Participant winner, LivingEntity target) {
        if (!(winner.entity instanceof ServerPlayer player)
                || !(winner.strike instanceof StrikeAttackActiveAccessor active)
                || target == null || !target.isAlive()) return;

        activeStrikes().put(player.getUUID(), winner.strike);
        FlyingStrikeYLock.begin(player);
        player.invulnerableTime = Math.max(player.invulnerableTime, 20);
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            data.getStatus().setStrikeLocked(true);
            NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
        });
        if (target instanceof ServerPlayer targetPlayer) {
            StatsProvider.get(StatsCapability.INSTANCE, targetPlayer).ifPresent(data -> {
                data.getStatus().setStrikeLocked(true);
                NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(targetPlayer), targetPlayer);
            });
        }

        NetworkHandler.sendToTrackingEntityAndSelf(new TriggerAnimationS2C(
                player.getUUID(), TriggerAnimationS2C.AnimationType.KI_ANIMATION,
                0, -1, active.dmzrevamp$getAnimationId()), player);
        StrikeAttackHandlerStateAccessor.dmzrevamp$invokeApplyStrikeDamage(
                player, target, active.dmzrevamp$getPerHitDamage(), active.dmzrevamp$getTechniqueId(), false);
    }

    private enum Result { ONGOING, A_WINS, B_WINS, DISSOLVED }

    private static final class Participant {
        private final LivingEntity entity;
        private final Object strike;
        private final boolean wasNoAi;
        private final int lockedNpcComboTimer;
        private final double initialMeleeDamage;
        private float meterPhase;
        private float previousMeterPhase;
        private float momentum;
        private Vec3 lockedPosition;
        private int visualComboCount;

        private Participant(LivingEntity entity, Object strike) {
            this.entity = entity;
            this.strike = strike;
            this.wasNoAi = entity instanceof Mob mob && mob.isNoAi();
            this.lockedNpcComboTimer = entity instanceof DBSagasEntity saga ? saga.comboTimer : 0;
            this.initialMeleeDamage = meleeDamage(entity);
            this.meterPhase = entity.getRandom().nextFloat();
            this.previousMeterPhase = meterPhase;
        }

        private void tickMeter(Participant opponent) {
            StrikeClashConfigured.Config config = StrikeClashConfigured.get();
            previousMeterPhase = meterPhase;
            meterPhase += config.meterSpeedPerTick;
            momentum *= config.momentumDecayPerTick;
            if (meterPhase >= 1F) meterPhase -= 1F;
            if (!(entity instanceof ServerPlayer)) {
                float[] area = goodArea(opponent);
                float center = (area[0] + area[1]) * 0.5F;
                if (previousMeterPhase < center && meterPhase >= center && meterPhase >= previousMeterPhase) {
                    press(opponent);
                }
            }
        }

        private void press(Participant opponent) {
            float[] area = goodArea(opponent);
            float efficiency = score(meterPhase, area[0], area[1]);
            StrikeClashConfigured.Config config = StrikeClashConfigured.get();
            double influence = 1D;
            if (config.meleeDMGInfluence) {
                double ratio = meleeDamage(entity) / Math.max(0.0001D, meleeDamage(opponent.entity));
                influence = Math.max(1D, 1D + (ratio - 1D) * config.meleeDMGInfluenceMultiplier);
            }
            momentum += efficiency * BURST_PER_PRESS * config.momentumGainDefaultMultiplier * (float) influence;
            meterPhase = 0F;
            previousMeterPhase = 0F;
        }

        private float[] goodArea(Participant opponent) {
            StrikeClashConfigured.Config config = StrikeClashConfigured.get();
            float low = config.goodAreaLow;
            float high = config.goodAreaHigh;
            if (!config.goodAreaSpeedInfluence) return new float[]{low, high};
            double ratio = speedFor(this) / Math.max(0.0001D, speedFor(opponent));
            if (ratio <= 1D) return new float[]{low, high};
            double factor = 1D + (ratio - 1D) * config.goodAreaSpeedInfluenceMultiplier;
            return new float[]{
                    Mth.clamp((float) (low / factor), 0F, low),
                    Mth.clamp((float) (1D - (1D - high) / factor), high, 1F)
            };
        }

        private static float score(float phase, float low, float high) {
            StrikeClashConfigured.Config config = StrikeClashConfigured.get();
            if (phase < low || phase > high) return config.offWindowMomentumEfficiency;
            float center = (low + high) * 0.5F;
            float half = Math.max(0.0001F, (high - low) * 0.5F);
            return config.offWindowMomentumEfficiency
                    + (1F - config.offWindowMomentumEfficiency)
                    * Math.max(0F, 1F - Math.abs(phase - center) / half);
        }
    }

    private static final class Clash {
        private final Participant a;
        private final Participant b;
        private float bias = 0.5F;
        private int age;

        private Clash(Participant a, Participant b) {
            this.a = a;
            this.b = b;
        }

        private boolean involves(UUID id) {
            return a.entity.getUUID().equals(id) || b.entity.getUUID().equals(id);
        }

        private Participant participant(UUID id) {
            if (a.entity.getUUID().equals(id)) return a;
            if (b.entity.getUUID().equals(id)) return b;
            return null;
        }

        private Participant other(Participant participant) {
            return participant == a ? b : a;
        }

        private void alignAndLock() {
            Participant higher = a.entity.getY() >= b.entity.getY() ? a : b;
            Participant lower = higher == a ? b : a;
            Vec3 direction = lower.entity.position().subtract(higher.entity.position()).multiply(1D, 0D, 1D);
            if (direction.lengthSqr() < 1.0E-6D) {
                direction = higher.entity.getLookAngle().multiply(1D, 0D, 1D);
            }
            if (direction.lengthSqr() < 1.0E-6D) direction = new Vec3(0D, 0D, 1D);
            direction = direction.normalize();
            higher.lockedPosition = higher.entity.position();
            lower.lockedPosition = higher.lockedPosition.add(direction.x, 0D, direction.z);
            lockAt(higher.entity, higher.lockedPosition);
            lockAt(lower.entity, lower.lockedPosition);
            if (higher.entity instanceof ServerPlayer player) FlyingStrikeYLock.updateAnchorAfterStrikeTeleport(player);
            if (lower.entity instanceof ServerPlayer player) FlyingStrikeYLock.updateAnchorAfterStrikeTeleport(player);
            if (a.entity instanceof Mob mob) mob.setNoAi(true);
            if (b.entity instanceof Mob mob) mob.setNoAi(true);
            face(a.entity, b.entity);
            face(b.entity, a.entity);
        }

        private Result tick(ServerLevel level) {
            if (!StrikeClashConfigured.get().enabled) return Result.DISSOLVED;
            if (!a.entity.isAlive() || !b.entity.isAlive()) return Result.DISSOLVED;
            if (a.entity instanceof DBSagasEntity saga && !saga.isComboing()) return Result.DISSOLVED;
            if (b.entity instanceof DBSagasEntity saga && !saga.isComboing()) return Result.DISSOLVED;

            age++;
            lockAt(a.entity, a.lockedPosition);
            lockAt(b.entity, b.lockedPosition);
            if (a.entity instanceof DBSagasEntity saga) saga.comboTimer = a.lockedNpcComboTimer;
            if (b.entity instanceof DBSagasEntity saga) saga.comboTimer = b.lockedNpcComboTimer;
            face(a.entity, b.entity);
            face(b.entity, a.entity);
            a.tickMeter(b);
            b.tickMeter(a);

            double powerA = meleeDamage(a.entity);
            double powerB = meleeDamage(b.entity);
            float shareA = (float) (powerA / Math.max(0.0001D, powerA + powerB));
            float shareB = 1F - shareA;
            float tractionA = Mth.clamp(2F * bias, MIN_TRACTION, 1F);
            float tractionB = Mth.clamp(2F * (1F - bias), MIN_TRACTION, 1F);
            float pushA = a.momentum * (POWER_FLOOR + POWER_SPAN * shareA) * tractionA;
            float pushB = b.momentum * (POWER_FLOOR + POWER_SPAN * shareB) * tractionB;
            bias = Mth.clamp(bias + DRIFT_PER_TICK * (pushA - pushB), 0F, 1F);

            renderCombat(level, a);
            renderCombat(level, b);
            if (age % 5 == 0) playPunch(age % 10 == 0 ? a.entity : b.entity);
            sendState(this, a);
            sendState(this, b);

            StrikeClashConfigured.Config config = StrikeClashConfigured.get();
            if (bias >= config.innerAdvantageHigh) return Result.A_WINS;
            if (bias <= config.innerAdvantageLow) return Result.B_WINS;
            if (age >= config.maxClashDurationTicks) {
                if (bias > 0.5F) return Result.A_WINS;
                if (bias < 0.5F) return Result.B_WINS;
                return Result.DISSOLVED;
            }
            return Result.ONGOING;
        }

        private void renderCombat(ServerLevel level, Participant participant) {
            int interval = visualAttackInterval(participant.entity);
            if (age % interval != 0) return;
            playBasicAttackAnimation(participant);
            double angle = level.random.nextDouble() * Math.PI * 2D;
            double radius = level.random.nextDouble() * 3D;
            double x = (a.entity.getX() + b.entity.getX()) * 0.5D + Math.cos(angle) * radius;
            double y = (a.entity.getEyeY() + b.entity.getEyeY()) * 0.5D + (level.random.nextDouble() - 0.5D) * 2D;
            double z = (a.entity.getZ() + b.entity.getZ()) * 0.5D + Math.sin(angle) * radius;
            level.sendParticles(MainParticles.PUNCH_PARTICLE.get(), x, y, z, 0,
                    0D, 0D, 0D, 1D);
        }

        private void playBasicAttackAnimation(Participant participant) {
            if (!(participant.entity instanceof ServerPlayer player)) {
                participant.entity.swing(InteractionHand.MAIN_HAND, true);
                return;
            }

            var hand = PlayerAttackHelper.getCurrentAttack(player, participant.visualComboCount);
            if (hand == null) {
                participant.visualComboCount = 0;
                hand = PlayerAttackHelper.getCurrentAttack(player, 0);
            }
            if (hand == null || hand.attack() == null) {
                player.swing(InteractionHand.MAIN_HAND, true);
                return;
            }

            float cooldownTicks = PlayerAttackHelper.getAttackCooldownTicksCapped(player);
            float animationSpeed = Mth.clamp(12F / Math.max(cooldownTicks, 0.001F), 0.55F, 1.35F);
            InteractionHand interactionHand = hand.isOffHand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            player.swing(interactionHand, true);
            NetworkHandler.sendToTrackingEntityAndSelf(new MeleeAnimationS2C(
                    player.getId(), hand.attack().animation(), hand.isOffHand(), animationSpeed), player);
            participant.visualComboCount++;
        }

        private float advantageFor(Participant participant) {
            return participant == a ? bias : 1F - bias;
        }

        private void finish(Result result) {
            notifyEnded(a.entity);
            notifyEnded(b.entity);
            restoreAi(a);
            restoreAi(b);
            if (result == Result.DISSOLVED) {
                if (a.entity instanceof ServerPlayer) stopPlayerStrike(a);
                if (b.entity instanceof ServerPlayer) stopPlayerStrike(b);
                return;
            }

            Participant winner = result == Result.A_WINS ? a : b;
            Participant loser = winner == a ? b : a;
            if (loser.entity instanceof ServerPlayer) stopPlayerStrike(loser);
            else if (loser.entity instanceof DBSagasEntity saga) saga.stopCombo();

            loser.entity.addEffect(new MobEffectInstance(MainEffects.STUN.get(), 40, 0, false, true, true));
            double liveDamageRatio = meleeDamage(winner.entity)
                    / Math.max(0.0001D, winner.initialMeleeDamage);
            float finalDamageMultiplier = (float) Math.min(Float.MAX_VALUE,
                    StrikeClashConfigured.get().winnerDamageIncreaseMultiplier
                            * (Double.isFinite(liveDamageRatio) ? Math.max(0D, liveDamageRatio) : 1D));
            WINNER_DAMAGE_BOOST.put(winner.entity.getUUID(), finalDamageMultiplier);
            WINNER_DAMAGE_ENTITY.put(winner.entity.getUUID(), winner.entity);
            restartWinningPlayerStrike(winner, loser.entity);
        }

        private static void restoreAi(Participant participant) {
            if (participant.entity instanceof Mob mob) mob.setNoAi(participant.wasNoAi);
        }
    }
}
