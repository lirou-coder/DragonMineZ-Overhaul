package com.dmzrevamp.revamp.ki;

import com.dmzrevamp.DmzRevampMod;
import com.dmzrevamp.config.KiClashConfigured;
import com.dmzrevamp.revamp.quest.QuestSpawnAttributeApplier;
import com.dragonminez.common.combat.clash.BeamClash;
import com.dragonminez.common.combat.clash.ClashParticipant;
import com.dragonminez.common.init.EntityAttributes;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.dragonminez.common.init.entities.ki.KiBlastEntity;
import com.dragonminez.common.init.entities.ki.KiDiskEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.BeamClashStateS2C;
import com.dragonminez.common.network.S2C.ProgressionSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.techniques.KiAttackData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class KiClashTeams {
    private static final Map<BeamClash, TeamState> STATES = Collections.synchronizedMap(new IdentityHashMap<>());
    private static final Map<UUID, Helper> HELPERS = new java.util.HashMap<>();
    private static final Map<UUID, MovementLock> MOVEMENT_LOCKS = new java.util.HashMap<>();
    private static final Map<AbstractKiProjectile, SphereLock> SPHERE_LOCKS = new IdentityHashMap<>();
    private static final Map<AbstractKiProjectile, FrozenLifetime> FROZEN_LIFETIMES = new IdentityHashMap<>();

    private KiClashTeams() {}

    public static boolean isHelper(UUID playerId) {
        synchronized (STATES) {
            return HELPERS.containsKey(playerId);
        }
    }

    /** True only during the finite post-loss punishment, not during the clash movement hold. */
    public static boolean isAbilityRestricted(LivingEntity entity) {
        synchronized (MOVEMENT_LOCKS) {
            MovementLock lock = MOVEMENT_LOCKS.get(entity.getUUID());
            return lock != null && lock.untilTick != Long.MAX_VALUE
                    && entity.level().getGameTime() < lock.untilTick;
        }
    }

    public static void tick(ServerLevel level, List<BeamClash> clashes) {
        Set<BeamClash> active = Collections.newSetFromMap(new IdentityHashMap<>());
        active.addAll(clashes);
        boolean discoverInThisLevel;
        synchronized (STATES) {
            Iterator<Map.Entry<BeamClash, TeamState>> iterator = STATES.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<BeamClash, TeamState> entry = iterator.next();
                if (!active.contains(entry.getKey()) || entry.getKey().isEnded()) {
                    close(entry.getValue());
                    iterator.remove();
                }
            }
            for (BeamClash clash : clashes) STATES.computeIfAbsent(clash, TeamState::new);
            for (TeamState state : STATES.values()) {
                preserveFullRender(state.clash.a().beam());
                preserveFullRender(state.clash.b().beam());
            }
            if (STATES.isEmpty()) {
                return;
            }
            discoverInThisLevel = false;
            for (TeamState state : STATES.values()) {
                if (state.clash.a().owner().level() == level) {
                    discoverInThisLevel = true;
                    break;
                }
            }
        }
        if (!KiClashConfigured.get().AllowHelpers) {
            synchronized (STATES) {
                for (TeamState state : STATES.values()) close(state, false);
            }
            return;
        }
        List<AbstractKiProjectile> projectiles = discoverInThisLevel
                ? KiProjectileIndex.snapshot(level).stream()
                        .filter(projectile -> !projectile.isRemoved() && projectile.isFiring())
                        .toList()
                : List.of();
        synchronized (STATES) {
            for (TeamState state : STATES.values()) {
                if (state.clash.a().owner().level() == level) {
                    holdMovement(state.clash.a().owner());
                    holdMovement(state.clash.b().owner());
                    discoverHelpers(state, projectiles);
                }
                // DMZ advances its global ACTIVE_CLASHES list once for every
                // ServerLevel END tick, not only for the clash's dimension.
                // Helpers must use that same global clock or their meter runs
                // N times slower, where N is the number of loaded dimensions.
                tickHelpers(state);
            }
        }
    }

    /**
     * Sends helper HUD state only after DMZ has sent every native clash snapshot
     * for this server tick. ClientBeamClashState is a single global value, so a
     * later native snapshot for the same player would otherwise replace the
     * helper phase and make the meter appear to crawl or oscillate.
     */
    public static void syncHelpers() {
        synchronized (STATES) {
            for (TeamState state : STATES.values()) {
                for (Helper helper : state.helpers) {
                    if (helper.owner instanceof ServerPlayer player
                            && helper.owner.isAlive()
                            && !helper.projectile.isRemoved()
                            && helper.projectile.isFiring()) {
                        syncHelper(helper, player);
                    }
                }
            }
        }
    }

    private static void discoverHelpers(TeamState state, List<AbstractKiProjectile> projectiles) {
        for (AbstractKiProjectile projectile : projectiles) {
            if (!KiClashAttackResolver.isAllowed(projectile) || projectile.isClashLocked() || !(projectile.getOwner() instanceof LivingEntity owner)) continue;
            if (state.hasOwner(owner.getUUID()) || HELPERS.containsKey(owner.getUUID())) continue;
            ClashParticipant hit = hitsClashingAttack(projectile, state.clash.a()) ? state.clash.a()
                    : hitsClashingAttack(projectile, state.clash.b()) ? state.clash.b() : null;
            if (hit == null) continue;
            ClashParticipant team = closestFacingTeam(owner, state.clash.a(), state.clash.b());
            Helper helper = new Helper(state, team, owner, projectile);
            state.helpers.add(helper);
            HELPERS.put(owner.getUUID(), helper);
            projectile.setClashLock(Math.max(1F, projectile.getClashBeamLength()), hit.owner().getUUID());
            preserveFullRender(projectile);
        }
    }

    private static ClashParticipant closestFacingTeam(LivingEntity helper, ClashParticipant first, ClashParticipant second) {
        Vec3 helperLook = normalizedLook(helper);
        double firstAlignment = helperLook.dot(normalizedLook(first.owner()));
        double secondAlignment = helperLook.dot(normalizedLook(second.owner()));
        return firstAlignment >= secondAlignment ? first : second;
    }

    private static Vec3 normalizedLook(LivingEntity entity) {
        Vec3 look = entity.getLookAngle();
        return look.lengthSqr() > 1.0E-9D ? look.normalize() : Vec3.directionFromRotation(entity.getXRot(), entity.getYRot());
    }

    /**
     * Uses each attack's real collision shape. Solid projectiles contribute the
     * segment swept by their original entity hitbox during this tick, while beam
     * attacks contribute their native rendered/collision segment.
     */
    private static boolean hitsClashingAttack(AbstractKiProjectile helper, ClashParticipant target) {
        AbstractKiProjectile clashing = target.beam();
        if (helper == clashing || helper.isRemoved() || clashing.isRemoved()) return false;

        Vec3 helperStart = attackStart(helper);
        Vec3 helperEnd = attackEnd(helper);
        Vec3 targetStart = attackStart(clashing);
        Vec3 targetEnd = attackEnd(clashing);
        double radius = Math.max(0.25D, (helper.getSize() + clashing.getSize()) * 0.5D) + 1.5D;
        return segmentDistanceSq(helperStart, helperEnd, targetStart, targetEnd) <= radius * radius;
    }

    private static Vec3 attackStart(AbstractKiProjectile projectile) {
        if (!isSolidClashProjectile(projectile)) return projectile.position();
        Vec3 movement = projectile.getDeltaMovement();
        return movement.lengthSqr() > 1.0E-9D ? projectile.position().subtract(movement) : projectile.position();
    }

    private static Vec3 attackEnd(AbstractKiProjectile projectile) {
        if (isSolidClashProjectile(projectile)) return projectile.position();
        return projectile.position().add(direction(projectile).scale(Math.max(0.1F, projectile.getClashBeamLength())));
    }

    private static void tickHelpers(TeamState state) {
        Iterator<Helper> iterator = state.helpers.iterator();
        while (iterator.hasNext()) {
            Helper helper = iterator.next();
            if (!helper.owner.isAlive() || helper.projectile.isRemoved() || !helper.projectile.isFiring()) {
                detach(helper);
                iterator.remove();
                continue;
            }
            preserveFullRender(helper.projectile);
            holdMovement(helper.owner);
            setMomentum(helper.participant, momentum(helper.team));
            helper.participant.tickMeter();
            setMomentum(helper.participant, momentum(helper.team));
        }
    }

    public static boolean handleHelperPress(ServerPlayer player) {
        Helper helper;
        synchronized (STATES) { helper = HELPERS.get(player.getUUID()); }
        if (helper == null) return false;
        helper.participant.registerPlayerPress();
        return true;
    }

    /** Replaces DMZ's private addBurst so every participant on one side shares one momentum value. */
    public static void applyMomentumBurst(ClashParticipant source, float efficiency) {
        float adjusted = efficiency
                * KiClashConfigured.get().momentumGainDefaultMultiplier
                * kiDamageInfluence(source);
        float delta = adjusted * 0.6F;
        TeamState state = findState(source);
        if (state == null) {
            setMomentum(source, momentum(source) + delta);
            return;
        }
        ClashParticipant team = state.teamFor(source);
        state.setTeamMomentum(team, momentum(team) + delta);
    }

    public static float adjustedMomentumDecay(ClashParticipant participant, float baseDecay) {
        int helpers = helperCount(participant);
        float reduction = Math.min(KiClashConfigured.get().MaxMomentumLossPerHelper,
                helpers * KiClashConfigured.get().MomentumLossReducePerHelper);
        return 1F - ((1F - baseDecay) * (1F - reduction));
    }

    public static float kiDamageInfluence(ClashParticipant participant) {
        var config = KiClashConfigured.get();
        if (!config.KiDMGInfluence && !config.overchargeInfluence) return 1F;
        TeamState state = findState(participant);
        if (state == null) return 1F;
        ClashParticipant team = state.teamFor(participant);
        ClashParticipant opponent = state.clash.a() == team ? state.clash.b() : state.clash.a();
        double influence = 1D;
        if (config.KiDMGInfluence) {
            double ratio = teamKiDamage(state, team) / Math.max(0.0001D, teamKiDamage(state, opponent));
            influence *= scaleInfluenceRatio(ratio, config.KiDMGInfluenceMultiplier);
        }
        if (config.overchargeInfluence) {
            double ratio = teamAverageCharge(state, team) / Math.max(0.0001D, teamAverageCharge(state, opponent));
            influence *= scaleInfluenceRatio(ratio, config.overchargeInfluenceMultiplier);
        }
        return (float) Math.max(1D, influence);
    }

    private static double scaleInfluenceRatio(double ratio, float multiplier) {
        return Math.max(0.0001D, 1D + (ratio - 1D) * multiplier);
    }

    private static double teamKiDamage(TeamState state, ClashParticipant participant) {
        double total = attackWeightedKiDamage(participant.owner(), participant.beam());
        for (Helper helper : state.helpers) if (helper.team == participant) {
            total += attackWeightedKiDamage(helper.owner, helper.projectile);
        }
        return Math.max(0.0001D, total);
    }

    private static double teamAverageCharge(TeamState state, ClashParticipant participant) {
        var config = KiClashConfigured.get();
        double weight = config.KiDMGInfluence
                ? attackWeightedKiDamage(participant.owner(), participant.beam()) : 1D;
        double weightedCharge = weight * chargeMultiplier(participant.beam());
        double totalWeight = weight;
        for (Helper helper : state.helpers) if (helper.team == participant) {
            weight = config.KiDMGInfluence
                    ? attackWeightedKiDamage(helper.owner, helper.projectile) : 1D;
            weightedCharge += weight * chargeMultiplier(helper.projectile);
            totalWeight += weight;
        }
        return weightedCharge / Math.max(0.0001D, totalWeight);
    }

    public static double effectivePower(LivingEntity entity, AbstractKiProjectile projectile) {
        var config = KiClashConfigured.get();
        double base = config.KiDMGInfluence ? attackWeightedKiDamage(entity, projectile) : 1D;
        double charge = config.overchargeInfluence ? chargeMultiplier(projectile) : 1D;
        return Math.max(0.0001D, base * charge);
    }

    public static boolean cancelIfTooStrong(AbstractKiProjectile first, AbstractKiProjectile second) {
        var config = KiClashConfigured.get();
        if (!config.cancelIffTooStrong || !(first.getOwner() instanceof LivingEntity firstOwner)
                || !(second.getOwner() instanceof LivingEntity secondOwner)) return false;
        double firstPower = effectivePower(firstOwner, first);
        double secondPower = effectivePower(secondOwner, second);
        double ratio = Math.max(firstPower, secondPower) / Math.max(0.0001D, Math.min(firstPower, secondPower));
        if (ratio < config.cancelMulti) return false;
        AbstractKiProjectile weaker = firstPower <= secondPower ? first : second;
        weaker.discard();
        return true;
    }

    public static void applyWinningHelperDamage(BeamClash clash, ClashParticipant winner) {
        if (winner == null) return;
        synchronized (STATES) {
            TeamState state = STATES.get(clash);
            if (state != null && state.winnerDamageApplied) return;

            // Projectiles normally retain the Ki Damage captured when they were
            // fired. Rebuild the winner from the owner's live Ki Damage so a
            // release/form change made during the clash is reflected at resolve.
            double finalDamage = refreshedProjectileDamage(winner.owner(), winner.beam());
            double helperSize = 0D;
            if (state != null) {
                for (Helper helper : state.helpers) {
                    if (helper.team == winner && helper.owner.isAlive()
                            && !helper.projectile.isRemoved()) {
                        finalDamage += refreshedProjectileDamage(helper.owner, helper.projectile);
                        helperSize += Math.max(0D, helper.projectile.getSize());
                    }
                }
            }

            if (finalDamage > 0D && Double.isFinite(finalDamage)) {
                winner.beam().setKiDamage((float) Math.min(Float.MAX_VALUE, finalDamage));
            }
            double additionalSize = helperSize * KiClashConfigured.get().helpersSizeIncreaseMultiplier;
            if (additionalSize > 0D && Double.isFinite(additionalSize)) {
                double finalSize = Math.max(0D, winner.beam().getSize()) + additionalSize;
                // AbstractKiProjectile#setSize updates synced render data and
                // refreshes entity dimensions, covering both visuals and hitbox.
                winner.beam().setSize((float) Math.min(Float.MAX_VALUE, finalSize));
            }
            if (state != null) state.winnerDamageApplied = true;
        }
    }

    public static boolean visualSphereClash(AbstractKiProjectile first, AbstractKiProjectile second) {
        if (!isSolidClashProjectile(first) && !isSolidClashProjectile(second)) return false;
        if (!KiClashAttackResolver.isAllowed(first) || !KiClashAttackResolver.isAllowed(second)
                || !KiClashAttackResolver.isLaunched(first) || !KiClashAttackResolver.isLaunched(second)) return false;
        Vec3 firstDirection = direction(first);
        Vec3 secondDirection = direction(second);
        if (firstDirection.dot(secondDirection) > -0.3D) return false;
        Vec3 firstMovement = first.getDeltaMovement();
        Vec3 secondMovement = second.getDeltaMovement();
        double firstReach = Math.max(firstMovement.length(), first.getSize() * 0.5D);
        double secondReach = Math.max(secondMovement.length(), second.getSize() * 0.5D);
        Vec3 firstStart = first.position().subtract(firstDirection.scale(firstReach));
        Vec3 firstEnd = first.position().add(firstDirection.scale(firstReach));
        Vec3 secondStart = second.position().subtract(secondDirection.scale(secondReach));
        Vec3 secondEnd = second.position().add(secondDirection.scale(secondReach));
        double visualRadius = Math.max(0.25D, (first.getSize() + second.getSize()) * 0.5D) + 1.5D;
        return segmentDistanceSq(firstStart, firstEnd, secondStart, secondEnd) <= visualRadius * visualRadius;
    }

    private static double segmentDistanceSq(Vec3 p1, Vec3 q1, Vec3 p2, Vec3 q2) {
        // Closest distance between two finite 3D segments.
        Vec3 d1 = q1.subtract(p1), d2 = q2.subtract(p2), r = p1.subtract(p2);
        double a = d1.dot(d1), e = d2.dot(d2), f = d2.dot(r), s, t;
        if (a <= 1.0E-9D && e <= 1.0E-9D) return p1.distanceToSqr(p2);
        if (a <= 1.0E-9D) { s = 0D; t = Math.max(0D, Math.min(1D, f / e)); }
        else {
            double c = d1.dot(r);
            if (e <= 1.0E-9D) { t = 0D; s = Math.max(0D, Math.min(1D, -c / a)); }
            else {
                double b = d1.dot(d2), denominator = a * e - b * b;
                s = denominator == 0D ? 0D : Math.max(0D, Math.min(1D, (b * f - c * e) / denominator));
                t = (b * s + f) / e;
                if (t < 0D) { t = 0D; s = Math.max(0D, Math.min(1D, -c / a)); }
                else if (t > 1D) { t = 1D; s = Math.max(0D, Math.min(1D, (b - c) / a)); }
            }
        }
        return p1.add(d1.scale(s)).distanceToSqr(p2.add(d2.scale(t)));
    }

    public static boolean tickClashingSolidProjectile(AbstractKiProjectile projectile) {
        if (!projectile.isClashLocked()) return false;
        projectile.setDeltaMovement(Vec3.ZERO);
        if (!projectile.level().isClientSide()) {
            synchronized (STATES) {
                SphereLock lock = SPHERE_LOCKS.get(projectile);
                if (lock != null) {
                    float currentLockedLength = Math.max(0F, projectile.getClashLockedLength());
                    float momentumMovement = currentLockedLength - lock.previousLockedLength;
                    Vec3 position = projectile.position().add(lock.direction.scale(momentumMovement));
                    projectile.setPos(position.x, position.y, position.z);
                    projectile.refreshDimensions();
                    lock.previousLockedLength = currentLockedLength;
                    projectile.hasImpulse = true;
                }
            }
        }
        return true;
    }

    private static boolean isSolidClashProjectile(AbstractKiProjectile projectile) {
        return projectile instanceof KiBlastEntity || projectile instanceof KiDiskEntity;
    }

    private static double chargeMultiplier(AbstractKiProjectile projectile) {
        float percent = projectile.getPersistentData().getFloat(KiAttackOverhaulEvents.OVERCHARGE_PERCENT_TAG);
        return Math.max(0.0001D, (percent > 0F && Float.isFinite(percent) ? percent : 100F) / 100D);
    }

    private static double entityKiDamage(LivingEntity entity, double projectileFallback) {
        double player = StatsProvider.get(StatsCapability.INSTANCE, entity).map(data -> data.getKiDamage()).orElse(0D);
        if (player > 0D) return player;
        double quest = QuestSpawnAttributeApplier.questKiDamageValue(entity);
        AttributeInstance attribute = entity.getAttribute(EntityAttributes.KI_BLAST_DAMAGE.get());
        double attributeValue = attribute == null ? 0D : attribute.getValue();
        double configured = Math.max(quest, attributeValue);
        return configured > 0D ? configured : Math.max(0.0001D, projectileFallback);
    }

    /** Current user Ki Damage multiplied by the technique's original damage percentage. */
    private static double attackWeightedKiDamage(LivingEntity entity, AbstractKiProjectile projectile) {
        double base = entityKiDamage(entity, projectile.getKiDamage());
        return Math.max(0.0001D, base * attackDamageMultiplier(entity, projectile, base));
    }

    /** Complete live projectile damage: current Ki Damage, technique output and launch charge. */
    private static double refreshedProjectileDamage(LivingEntity entity, AbstractKiProjectile projectile) {
        return Math.max(0.0001D, attackWeightedKiDamage(entity, projectile) * chargeMultiplier(projectile));
    }

    private static double attackDamageMultiplier(LivingEntity entity, AbstractKiProjectile projectile, double base) {
        String techniqueId = projectile.getTechniqueId();
        double configured = StatsProvider.get(StatsCapability.INSTANCE, entity).map(data -> {
            Object technique = data.getTechniques().getUnlockedTechniques().get(techniqueId);
            if (technique instanceof KiAttackData ki) {
                return (double) ki.getDamageMultiplier()
                        * ki.getConfiguredDamageMultiplier()
                        * ki.getOutputMultiplier();
            }
            return 0D;
        }).orElse(0D);
        if (configured > 0D && Double.isFinite(configured)) return configured;

        // Story/AI attacks may not expose KiAttackData. Their projectile damage is
        // already base Ki Damage * technique multiplier * launch charge, so derive
        // and retain the technique portion while overcharge is compared separately.
        double inferred = projectile.getKiDamage()
                / Math.max(0.0001D, base * chargeMultiplier(projectile));
        return Double.isFinite(inferred) && inferred > 0D ? inferred : 1D;
    }

    private static int helperCount(ClashParticipant participant) {
        TeamState state = findState(participant);
        if (state == null) return 0;
        ClashParticipant team = state.teamFor(participant);
        int count = 0;
        for (Helper helper : state.helpers) if (helper.team == team) count++;
        return count;
    }

    private static TeamState findState(ClashParticipant participant) {
        synchronized (STATES) {
            for (TeamState state : STATES.values()) {
                if (state.clash.a() == participant || state.clash.b() == participant
                        || state.helpers.stream().anyMatch(helper -> helper.participant == participant)) return state;
            }
        }
        return null;
    }

    private static float momentum(ClashParticipant participant) {
        return ((ClashParticipantAccess) participant).dmzrevamp$getMomentum();
    }

    private static void setMomentum(ClashParticipant participant, float momentum) {
        ((ClashParticipantAccess) participant).dmzrevamp$setMomentum(momentum);
    }

    private static void syncHelper(Helper helper, ServerPlayer player) {
        ClashParticipant opponent = helper.state.clash.a() == helper.team ? helper.state.clash.b() : helper.state.clash.a();
        NetworkHandler.sendToPlayer(
                new BeamClashStateS2C(true, helper.participant.meterPhase(),
                        KiClashConfigured.get().goodAreaLow, KiClashConfigured.get().goodAreaHigh,
                        helper.state.clash.advantageFor(helper.team.owner()),
                        helper.projectile.getColorBorder(), opponent.owner().getId()), player);
    }

    /** DMZ renderers fade from tickCount/maxLife, so maxLife itself stays frozen far ahead. */
    private static void preserveFullRender(AbstractKiProjectile projectile) {
        if (projectile.isRemoved()) return;
        FrozenLifetime lifetime = FROZEN_LIFETIMES.computeIfAbsent(projectile, attack -> {
            int remaining = Math.max(1, attack.getMaxLife() - attack.tickCount);
            return new FrozenLifetime(remaining);
        });
        projectile.setMaxLife(Integer.MAX_VALUE);
    }

    private static void releaseFrozenLifetime(AbstractKiProjectile projectile) {
        FrozenLifetime lifetime = FROZEN_LIFETIMES.remove(projectile);
        if (lifetime != null && !projectile.isRemoved()) {
            projectile.setMaxLife(projectile.tickCount + lifetime.originalRemaining);
        }
    }

    private static float scoreEfficiency(float phase) {
        var config = KiClashConfigured.get();
        if (phase < config.goodAreaLow || phase > config.goodAreaHigh) return config.offWindowMomentumEfficiency;
        float center = (config.goodAreaLow + config.goodAreaHigh) * 0.5F;
        float half = Math.max(0.0001F, (config.goodAreaHigh - config.goodAreaLow) * 0.5F);
        return config.offWindowMomentumEfficiency + (1F - config.offWindowMomentumEfficiency) * Math.max(0F, 1F - Math.abs(phase - center) / half);
    }

    private static Vec3 direction(AbstractKiProjectile projectile) {
        Vec3 movement = projectile.getDeltaMovement();
        return movement.lengthSqr() > 1.0E-6D ? movement.normalize() : Vec3.directionFromRotation(projectile.getClashPitch(), projectile.getClashYaw());
    }

    private static void close(TeamState state) { close(state, true); }

    private static void close(TeamState state, boolean discardProjectile) {
        releaseFrozenLifetime(state.clash.a().beam());
        releaseFrozenLifetime(state.clash.b().beam());
        restoreSolidProjectile(state.clash.a().beam());
        restoreSolidProjectile(state.clash.b().beam());
        releaseActiveMovement(state.clash.a().owner());
        releaseActiveMovement(state.clash.b().owner());
        for (Helper helper : state.helpers) {
            detach(helper);
            if (discardProjectile && !helper.projectile.isRemoved()) helper.projectile.discard();
        }
        state.helpers.clear();
    }

    private static void restoreSolidProjectile(AbstractKiProjectile projectile) {
        SphereLock lock = SPHERE_LOCKS.remove(projectile);
        if (lock == null || projectile.isRemoved()) return;
        Vec3 restored = lock.velocity.lengthSqr() > 1.0E-9D
                ? lock.velocity : lock.direction.scale(Math.max(0.08D, projectile.getKiSpeed()));
        projectile.setDeltaMovement(restored);
        projectile.hasImpulse = true;
    }

    private static void detach(Helper helper) {
        HELPERS.remove(helper.owner.getUUID());
        releaseMovement(helper.owner);
        releaseFrozenLifetime(helper.projectile);
        helper.projectile.clearClashLock();
        if (helper.owner instanceof ServerPlayer player) {
            NetworkHandler.sendToPlayer(BeamClashStateS2C.inactive(), player);
        }
    }

    @SubscribeEvent
    public static void protectHelpers(LivingAttackEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof LivingEntity living && isAbilityRestricted(living)) {
            event.setCanceled(true);
            return;
        }
        synchronized (STATES) { if (HELPERS.containsKey(event.getEntity().getUUID())) event.setCanceled(true); }
    }

    /** Rejects Ki attacks spawned by a punished player or mob, including native AI techniques. */
    @SubscribeEvent
    public static void blockRestrictedKiAttackSpawn(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof AbstractKiProjectile projectile)) return;
        if (projectile.getOwner() instanceof LivingEntity owner && isAbilityRestricted(owner)) {
            event.setCanceled(true);
        }
    }

    public static void immobilizeAfterLoss(LivingEntity entity, int ticks) {
        synchronized (MOVEMENT_LOCKS) {
            MovementLock current = MOVEMENT_LOCKS.get(entity.getUUID());
            boolean wasNoAi = current != null ? current.wasNoAi : entity instanceof Mob mob && mob.isNoAi();
            MOVEMENT_LOCKS.put(entity.getUUID(), new MovementLock(entity,
                    current == null ? entity.position() : current.anchor,
                    entity.level().getGameTime() + Math.max(1, ticks), wasNoAi));
            if (entity instanceof Mob mob) mob.setNoAi(true);
        }
        if (entity instanceof ServerPlayer player) {
            StatsProvider.get(StatsCapability.INSTANCE, player)
                    .ifPresent(data -> {
                        suppressActiveAbilities(data);
                        NetworkHandler.sendToTrackingEntityAndSelf(new ProgressionSyncS2C(player), player);
                    });
        }
    }

    private static void suppressActiveAbilities(com.dragonminez.common.stats.StatsData data) {
        data.getTechniques().clearTechniqueCharge();
        data.getStatus().setActionCharging(false);
        data.getStatus().setChargingKi(false);
        data.getStatus().setBlocking(false);
    }

    public static void releaseMovement(LivingEntity entity) {
        synchronized (MOVEMENT_LOCKS) {
            MovementLock removed = MOVEMENT_LOCKS.remove(entity.getUUID());
            restoreMobAi(removed);
        }
    }

    private static void releaseActiveMovement(LivingEntity entity) {
        synchronized (MOVEMENT_LOCKS) {
            MovementLock lock = MOVEMENT_LOCKS.get(entity.getUUID());
            if (lock != null && lock.untilTick == Long.MAX_VALUE) {
                MOVEMENT_LOCKS.remove(entity.getUUID());
                restoreMobAi(lock);
            }
        }
    }

    private static void holdMovement(LivingEntity entity) {
        synchronized (MOVEMENT_LOCKS) {
            MovementLock current = MOVEMENT_LOCKS.get(entity.getUUID());
            MOVEMENT_LOCKS.put(entity.getUUID(), new MovementLock(entity,
                    current == null ? entity.position() : current.anchor, Long.MAX_VALUE,
                    current != null ? current.wasNoAi : entity instanceof Mob mob && mob.isNoAi()));
        }
    }

    private static void restoreMobAi(MovementLock lock) {
        if (lock != null && lock.entity instanceof Mob mob && !lock.wasNoAi) mob.setNoAi(false);
    }

    @SubscribeEvent
    public static void lockMovement(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        MovementLock lock;
        synchronized (MOVEMENT_LOCKS) {
            lock = MOVEMENT_LOCKS.get(entity.getUUID());
            if (lock != null && lock.untilTick != Long.MAX_VALUE && entity.level().getGameTime() >= lock.untilTick) {
                MOVEMENT_LOCKS.remove(entity.getUUID());
                restoreMobAi(lock);
                lock = null;
            }
        }
        if (lock == null) return;
        if (lock.untilTick != Long.MAX_VALUE && entity instanceof ServerPlayer player) {
            StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(KiClashTeams::suppressActiveAbilities);
        }
        entity.setDeltaMovement(Vec3.ZERO);
        entity.teleportTo(lock.anchor.x, lock.anchor.y, lock.anchor.z);
        entity.fallDistance = 0F;
        entity.hasImpulse = true;
    }

    private static final class TeamState {
        final BeamClash clash;
        final List<Helper> helpers = new ArrayList<>();
        boolean winnerDamageApplied;
        TeamState(BeamClash clash) {
            this.clash = clash;
            rememberSphere(clash.a());
            rememberSphere(clash.b());
        }
        ClashParticipant teamFor(ClashParticipant participant) {
            if (participant == clash.a() || participant == clash.b()) return participant;
            for (Helper helper : helpers) if (helper.participant == participant) return helper.team;
            return participant;
        }
        void setTeamMomentum(ClashParticipant team, float value) {
            setMomentum(team, value);
            for (Helper helper : helpers) if (helper.team == team) setMomentum(helper.participant, value);
        }
        private static void rememberSphere(ClashParticipant participant) {
            if (isSolidClashProjectile(participant.beam())) {
                Vec3 velocity = participant.beam().getDeltaMovement();
                Vec3 physicalDirection = velocity.lengthSqr() > 1.0E-9D ? velocity.normalize() : participant.direction();
                SPHERE_LOCKS.put(participant.beam(), new SphereLock(
                        physicalDirection,
                        velocity,
                        Math.max(0F, participant.beam().getClashLockedLength())
                ));
            }
        }
        boolean hasOwner(UUID id) { return clash.involvesOwner(id) || helpers.stream().anyMatch(h -> h.owner.getUUID().equals(id)); }
    }

    private static final class Helper {
        final TeamState state;
        final ClashParticipant team;
        final LivingEntity owner;
        final AbstractKiProjectile projectile;
        final ClashParticipant participant;
        Helper(TeamState state, ClashParticipant team, LivingEntity owner, AbstractKiProjectile projectile) {
            this.state = state; this.team = team; this.owner = owner; this.projectile = projectile;
            this.participant = new ClashParticipant(projectile, owner);
            setMomentum(this.participant, momentum(team));
        }
    }

    private record MovementLock(LivingEntity entity, Vec3 anchor, long untilTick, boolean wasNoAi) {}
    private record FrozenLifetime(int originalRemaining) {}
    private static final class SphereLock {
        final Vec3 direction;
        final Vec3 velocity;
        float previousLockedLength;

        SphereLock(Vec3 direction, Vec3 velocity, float previousLockedLength) {
            this.direction = direction;
            this.velocity = velocity;
            this.previousLockedLength = previousLockedLength;
        }
    }
}
