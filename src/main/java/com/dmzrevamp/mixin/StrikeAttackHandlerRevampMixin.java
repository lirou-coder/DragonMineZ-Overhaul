package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.strike.RevampStrikeAttackData;
import com.dmzrevamp.revamp.strike.SleepRecoveryEvents;
import com.dmzrevamp.revamp.strike.NamekianRegenerationEvents;
import com.dmzrevamp.revamp.strike.StrikeAttackEffectApplier;
import com.dmzrevamp.revamp.strike.StrikeAttackTemplates;
import com.dmzrevamp.revamp.strike.CustomStrikeType;
import com.dmzrevamp.revamp.strike.FlyingStrikeYLock;
import com.dmzrevamp.revamp.strike.StrikeClashManager;
import com.dmzrevamp.revamp.strike.StrikeAttackDelayManager;
import com.dmzrevamp.revamp.combat.AdaptiveDefenseDamageContext;
import com.dragonminez.common.init.MainDamageTypes;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.ki.KiWaveEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.network.S2C.TriggerAnimationS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Cooldowns;
import com.dragonminez.common.stats.character.Resources;
import com.dragonminez.common.stats.character.Status;
import com.dragonminez.common.stats.techniques.StrikeAttackData;
import com.dragonminez.common.stats.techniques.TechniqueData;
import com.dragonminez.server.events.players.combat.StrikeAttackHandler;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.RegistryObject;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.lang.reflect.Method;

@Mixin(StrikeAttackHandler.class)
public abstract class StrikeAttackHandlerRevampMixin {
    @Shadow(remap = false)
    @Final
    private static Map<UUID, Object> ACTIVE;

    @Unique
    private static Method dmzrevamp$withTicksElapsedMethod;

    @Unique
    private static final Map<UUID, Integer> DMZREVAMP_EVASIVE_PUSH_TICK = new HashMap<>();

    @Redirect(
            method = "lambda$requestStrike$0",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/stats/character/Status;isStunned()Z"),
            remap = false,
            require = 1
    )
    private static boolean dmzrevamp$preserveStrikeThatWasAlreadyDelayed(Status status) {
        return StrikeAttackDelayManager.isBlockedDuringRequest(status);
    }

    @Redirect(
            method = "lambda$requestStrike$0",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;m_41619_()Z"),
            remap = false
    )
    private static boolean dmzrevamp$allowStrikeWithHeldItems(ItemStack stack) {
        return true;
    }

    @Redirect(
            method = "lambda$startStrike$4",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/stats/techniques/StrikeAttackData;getDamageMultiplier()F"),
            remap = false,
            require = 1
    )
    private static float dmzrevamp$useConfiguredCustomStrikeDamage(StrikeAttackData strike) {
        if (strike instanceof RevampStrikeAttackData revamp && revamp.dmzrevamp$isCustomStrike()) {
            return strike.getActualDamageMultiplier();
        }
        return strike.getDamageMultiplier();
    }

    @Redirect(
            method = "lambda$requestStrike$0",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/stats/techniques/StrikeAttackData;getAnimationId()Ljava/lang/String;"),
            remap = false
    )
    private static String dmzrevamp$normalizeCustomStrikeAnimationId(StrikeAttackData strike) {
        if (strike instanceof RevampStrikeAttackData revamp && revamp.dmzrevamp$isCustomStrike()) {
            CustomStrikeType type = revamp.dmzrevamp$getStrikeType();
            if (type != null) {
                return type.animationId();
            }
        }
        String animationId = strike.getAnimationId();
        if ("kaioken_attack".equals(animationId)) {
            return "skp.kaioken_attack";
        }
        if ("animation.technique.evasive".equals(animationId)) {
            return "technique.evasive";
        }
        return animationId;
    }

    @Redirect(
            method = "lambda$requestStrike$0",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/stats/character/Resources;getCurrentEnergy()F"),
            remap = false
    )
    private static float dmzrevamp$allowCreativeCustomStrikeWithoutEnergy(Resources resources) {
        if (resources != null && resources.getPlayer() instanceof ServerPlayer player && player.getAbilities().instabuild) {
            StatsData data = resources.getStatsData();
            if (dmzrevamp$isSelectedCustomStrike(data)) {
                return Float.MAX_VALUE;
            }
        }
        return resources.getCurrentEnergy();
    }

    @Redirect(
            method = "lambda$requestStrike$0",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/stats/character/Resources;removeEnergy(F)V"),
            remap = false
    )
    private static void dmzrevamp$skipCreativeCustomStrikeEnergyCost(Resources resources, float amount) {
        if (resources != null && resources.getPlayer() instanceof ServerPlayer player && player.getAbilities().instabuild) {
            StatsData data = resources.getStatsData();
            if (dmzrevamp$isSelectedCustomStrike(data)) {
                return;
            }
        }
        resources.removeEnergy(amount);
    }

    @Redirect(
            method = "lambda$failPending$6",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/stats/character/Cooldowns;setCooldown(Ljava/lang/String;I)V"),
            remap = false
    )
    private static void dmzrevamp$skipCreativeCustomStrikeMissCooldown(Cooldowns cooldowns, String key, int ticks, @Coerce Object pendingStrike, ServerPlayer player, StatsData data) {
        if (player != null && player.getAbilities().instabuild && dmzrevamp$isCustomStrikeCooldown(data, key)) {
            return;
        }
        cooldowns.setCooldown(key, ticks);
    }

    @Redirect(
            method = "lambda$failPending$6",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/stats/character/Resources;addEnergy(F)V"),
            remap = false
    )
    private static void dmzrevamp$skipCreativeCustomStrikeMissRefund(Resources resources, float amount, @Coerce Object pendingStrike, ServerPlayer player, StatsData data) {
        if (player != null && player.getAbilities().instabuild && resources != null && dmzrevamp$isCustomStrikeCooldown(data, dmzrevamp$pendingCooldownKey(pendingStrike))) {
            return;
        }
        resources.addEnergy(amount);
    }

    @Inject(method = "requestStrike", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dmzrevamp$handleInstantSleepRecovery(ServerPlayer player, int targetId, CallbackInfo ci) {
        if (player == null) {
            return;
        }
        if (StrikeAttackDelayManager.interceptPlayerRequest(player, targetId)) {
            ci.cancel();
            return;
        }
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            TechniqueData selected = data.getTechniques().getSelectedTechnique();
            if (!(selected instanceof StrikeAttackData strike)) {
                return;
            }
            if (StrikeAttackTemplates.NAMEKIAN_REGENERATION.equals(strike.getId())) {
                dmzrevamp$startNamekianRegeneration(player, data, strike, ci);
                return;
            }
            if (!StrikeAttackTemplates.SLEEP_RECOVERY.equals(strike.getId())) return;
            if (!data.getStatus().isHasCreatedCharacter() || data.getStatus().isStunned()) {
                ci.cancel();
                return;
            }
            String cooldownKey = "TechniqueCooldown_" + strike.getId();
            if (data.getCooldowns().hasCooldown(cooldownKey)) {
                ci.cancel();
                return;
            }
            double cost = strike.getCalculatedCost(data);
            if (!player.getAbilities().instabuild && data.getResources().getCurrentEnergy() < cost) {
                ci.cancel();
                return;
            }
            if (!player.getAbilities().instabuild) {
                data.getResources().removeEnergy((float) Math.ceil(cost));
                data.getCooldowns().setCooldown(cooldownKey, strike.getActualCooldown());
            }
            data.getStatus().setStrikeLocked(true);
            data.getCooldowns().setCooldown(SleepRecoveryEvents.LOCK_COOLDOWN, 100);
            SleepRecoveryEvents.markActive(player);
            player.setDeltaMovement(Vec3.ZERO);
            player.hurtMarked = true;
            player.heal((float) Math.max(1.0D, cost * 0.5D));
            dmzrevamp$play(player, MainSounds.KI_CHARGE_LOOP, 1.0F, 1.2F);
            if (player.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.2D, player.getZ(), 8, 0.45D, 0.5D, 0.45D, 0.05D);
            }
            NetworkHandler.sendToTrackingEntityAndSelf(new TriggerAnimationS2C(player.getUUID(), TriggerAnimationS2C.AnimationType.KI_ANIMATION, 0, player.getId(), "base.meditation"), player);
            NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
            ci.cancel();
        });
    }

    @Unique
    private static void dmzrevamp$startNamekianRegeneration(ServerPlayer player, StatsData data, StrikeAttackData strike, CallbackInfo ci) {
        if (!data.getStatus().isHasCreatedCharacter() || data.getStatus().isStunned() || data.getStatus().isStrikeLocked()) {
            ci.cancel();
            return;
        }
        String cooldownKey = "TechniqueCooldown_" + strike.getId();
        if (data.getCooldowns().hasCooldown(cooldownKey)) {
            ci.cancel();
            return;
        }
        double kiCost = strike.getCalculatedCost(data);
        if (!player.getAbilities().instabuild && data.getResources().getCurrentEnergy() < kiCost) {
            ci.cancel();
            return;
        }
        if (!player.getAbilities().instabuild) {
            data.getResources().removeEnergy((float) Math.ceil(kiCost));
            data.getCooldowns().setCooldown(cooldownKey, strike.getActualCooldown());
        }
        data.getStatus().setStrikeLocked(true);
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
        NamekianRegenerationEvents.start(player);
        NetworkHandler.sendToTrackingEntityAndSelf(new TriggerAnimationS2C(
                player.getUUID(), TriggerAnimationS2C.AnimationType.KI_ANIMATION, 0, player.getId(), "animation.technique.regeneration"), player);
        NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
        ci.cancel();
    }


    @Inject(method = "applyStrikeDamage", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dmzrevamp$overrideCustomStrikeDamage(ServerPlayer player, LivingEntity target, double damage, String techniqueId, boolean finalHit, CallbackInfo ci) {
        if (player == null || target == null || techniqueId == null) {
            return;
        }
        if (StrikeClashManager.tryStart(player, target)) {
            ci.cancel();
            return;
        }
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            TechniqueData technique = data.getTechniques().getUnlockedTechniques().get(techniqueId);
            if (!(technique instanceof StrikeAttackData strike) || !(strike instanceof RevampStrikeAttackData revamp) || !revamp.dmzrevamp$isCustomStrike()) {
                return;
            }
            if (revamp.dmzrevamp$getStrikeType().isEvasive()) {
                ci.cancel();
                return;
            }
            dmzrevamp$lockCustomStrikeTarget(player, target, data);
            ci.cancel();
        });
    }

    @Redirect(
            method = "applyStrikeDamage",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;m_6469_(Lnet/minecraft/world/damagesource/DamageSource;F)Z"
            ),
            remap = false,
            require = 1
    )
    private static boolean dmzrevamp$useWholeStrikeTechniqueForAdaptiveDefense(
            LivingEntity target,
            DamageSource source,
            float hitDamage,
            ServerPlayer player,
            LivingEntity originalTarget,
            double originalDamage,
            String techniqueId,
            boolean finalHit
    ) {
        Object active = ACTIVE.get(player.getUUID());
        double totalDamage = active instanceof StrikeAttackActiveAccessor accessor
                ? accessor.dmzrevamp$getTotalDamage()
                : Math.max(hitDamage, originalDamage);
        float scaledHitDamage = StrikeClashManager.scaleWinningPlayerDamage(player, hitDamage);
        double scaledTotalDamage = StrikeClashManager.scaleWinningPlayerDamage(player, totalDamage);
        return AdaptiveDefenseDamageContext.hurt(
                target,
                source,
                scaledHitDamage,
                AdaptiveDefenseDamageContext.AttackType.STRIKE,
                scaledTotalDamage
        );
    }

    @Inject(method = "processActiveInternal", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dmzrevamp$processCustomStrikeTimeline(ServerPlayer player, CallbackInfo ci) {
        if (player == null) {
            return;
        }
        if (StrikeClashManager.shouldPauseStrike(player)) {
            ci.cancel();
            return;
        }
        Object active = ACTIVE.get(player.getUUID());
        if (!(active instanceof StrikeAttackActiveAccessor activeData)) {
            return;
        }
        String techniqueId = activeData.dmzrevamp$getTechniqueId();
        if ("kaioken_attack".equals(techniqueId) && activeData.dmzrevamp$getTicksElapsed() < 15) {
            FlyingStrikeYLock.allowScriptedVerticalMotion(player);
        }
        StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).resolve().orElse(null);
        if (data == null) {
            return;
        }
        TechniqueData technique = data.getTechniques().getUnlockedTechniques().get(techniqueId);
        if (!(technique instanceof StrikeAttackData strike) || !(strike instanceof RevampStrikeAttackData revamp) || !revamp.dmzrevamp$isCustomStrike()) {
            return;
        }
        ci.cancel();
        UUID targetId = activeData.dmzrevamp$getTargetId();
        LivingEntity target = dmzrevamp$resolveLiving(player, targetId);
        if (target == null || !target.isAlive() || !player.isAlive()) {
            dmzrevamp$endCustomStrike(player, target, data);
            return;
        }
        int tick = activeData.dmzrevamp$getTicksElapsed() + 1;
        double totalDamage = Math.max(0.0D, activeData.dmzrevamp$getTotalDamage());
        if (revamp.dmzrevamp$getStrikeType().isEvasive()) {
            dmzrevamp$face(player, target);
            dmzrevamp$freeze(player);
            if (tick == 6) {
                dmzrevamp$pushTarget(player, target);
                StrikeAttackEffectApplier.applyExtras(strike, player, target);
            }
            if (tick >= 20) {
                dmzrevamp$endCustomStrike(player, target, data);
                return;
            }
            ACTIVE.put(player.getUUID(), dmzrevamp$withTicksElapsed(active, tick));
            return;
        }
        dmzrevamp$lockCustomStrikeTarget(player, target, data);
        dmzrevamp$face(player, target);
        if (target instanceof ServerPlayer targetPlayer) {
            dmzrevamp$face(targetPlayer, player);
        }
        boolean ended = dmzrevamp$runCustomComboTick(player, target, data, strike, revamp, tick, totalDamage);
        if (ended || tick >= Math.max(1, activeData.dmzrevamp$getDurationTicks())) {
            dmzrevamp$endCustomStrike(player, target, data);
            return;
        }
        ACTIVE.put(player.getUUID(), dmzrevamp$withTicksElapsed(active, tick));
    }

    @Inject(method = "startStrike", at = @At("HEAD"), remap = false)
    private static void dmzrevamp$startStrikeYLock(
            ServerPlayer player,
            LivingEntity target,
            @Coerce Object pending,
            CallbackInfo ci
    ) {
        FlyingStrikeYLock.begin(player);
    }

    @Inject(method = "startStrike", at = @At("RETURN"), remap = false)
    private static void dmzrevamp$finishStrikeClashAbort(
            ServerPlayer player,
            LivingEntity target,
            @Coerce Object pending,
            CallbackInfo ci
    ) {
        StrikeClashManager.finalizePlayerStrikeAbort(player);
    }

    @Inject(method = "endStrike", at = @At("HEAD"), remap = false)
    private static void dmzrevamp$finishStrikeYLock(
            ServerPlayer player,
            LivingEntity target,
            @Coerce Object active,
            CallbackInfo ci
    ) {
        FlyingStrikeYLock.finish(player);
    }

    @Unique
    private static Object dmzrevamp$withTicksElapsed(Object activeStrike, int ticksElapsed) {
        try {
            if (dmzrevamp$withTicksElapsedMethod == null) {
                dmzrevamp$withTicksElapsedMethod =
                        activeStrike.getClass().getDeclaredMethod(
                                "withTicksElapsed",
                                int.class
                        );

                dmzrevamp$withTicksElapsedMethod.setAccessible(true);
            }

            return dmzrevamp$withTicksElapsedMethod.invoke(
                    activeStrike,
                    ticksElapsed
            );
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(
                    "Failed to call ActiveStrike.withTicksElapsed",
                    e
            );
        }
    }

    @Unique
    private static boolean dmzrevamp$runCustomComboTick(ServerPlayer player, LivingEntity target, StatsData data, StrikeAttackData strike, RevampStrikeAttackData revamp, int tick, double totalDamage) {
        if (StrikeAttackTemplates.ANDROID_ABSORPTION.equals(strike.getId())) {
            if (tick == 1) {
                Vec3 look = target.getLookAngle().normalize();
                dmzrevamp$movePlayer(player, target.getX() + look.x * 0.8D, target.getY(), target.getZ() + look.z * 0.8D);
                dmzrevamp$play(player, MainSounds.TP, 1.0F, 1.0F);
            } else if (tick == 10 || tick == 20 || tick == 30) {
                dmzrevamp$absorbHit(player, target, (float) (totalDamage / 3.0D), strike, data);
            } else if (tick < 30) {
                Vec3 hold = player.position().add(player.getLookAngle().normalize().scale(0.6D));
                target.teleportTo(hold.x, hold.y, hold.z);
                dmzrevamp$freeze(target);
            } else if (tick >= 35) {
                dmzrevamp$setMotion(target, 1.5D, 0.5D, 1.5D);
                StrikeAttackEffectApplier.applyExtras(strike, player, target);
                return true;
            }
            return false;
        }
        switch (revamp.dmzrevamp$getStrikeType()) {
            case BASIC -> {
                float hit = (float) (totalDamage / 3.0D);
                if (tick == 1) {
                    dmzrevamp$teleportAndHit(player, target, target.getLookAngle().normalize(), 1.5D, hit, MainSounds.CRITICO1, 1.4F, false, strike, data);
                } else if (tick == 12) {
                    dmzrevamp$teleportAndHit(player, target, target.getLookAngle().normalize().scale(-1.0D), 1.5D, hit, MainSounds.CRITICO1, 1.5F, false, strike, data);
                } else if (tick == 22) {
                    Vec3 look = target.getLookAngle().normalize();
                    dmzrevamp$movePlayer(player, target.getX() + look.x * 1.5D, target.getY() + 0.5D, target.getZ() + look.z * 1.5D);
                    dmzrevamp$play(player, MainSounds.TP, 1.0F, 1.1F);
                } else if (tick == 31) {
                    dmzrevamp$finalBlow(player, target, hit, 2.5D, strike, data);
                    return true;
                }
            }
            case AIR -> {
                float hit = (float) (totalDamage / 2.0D);
                if (tick == 1) {
                    dmzrevamp$teleportAndHit(player, target, target.getLookAngle().normalize(), 1.5D, hit, MainSounds.CRITICO1, 1.2F, false, strike, data);
                    dmzrevamp$setMotion(target, 0.0D, 1.2D, 0.0D);
                } else if (tick == 12) {
                    Vec3 look = target.getLookAngle().normalize();
                    dmzrevamp$movePlayer(player, target.getX() + look.x * 0.5D, target.getY() + 2.5D, target.getZ() + look.z * 0.5D);
                    dmzrevamp$play(player, MainSounds.TP, 1.0F, 1.3F);
                } else if (tick == 20) {
                    dmzrevamp$hit(player, target, hit, strike, false, data);
                    dmzrevamp$play(player, MainSounds.CRITICO1, 1.0F, 0.8F);
                    dmzrevamp$spawnPunchParticles(player, target);
                    dmzrevamp$setMotion(target, 0.0D, -2.5D, 0.0D);
                    StrikeAttackEffectApplier.applyExtras(strike, player, target);
                    return true;
                }
            }
            case CHARGE -> {
                float hit = (float) (totalDamage / 2.0D);
                if (tick == 1) {
                    dmzrevamp$play(player, MainSounds.KI_CHARGE_LOOP, 1.0F, 1.5F);
                    Vec3 direction = target.position().subtract(player.position()).normalize();
                    player.setDeltaMovement(direction.scale(1.5D));
                    player.hurtMarked = true;
                } else if (tick == 6) {
                    dmzrevamp$hit(player, target, hit, strike, false, data);
                    dmzrevamp$spawnPunchParticles(player, target);
                    dmzrevamp$freeze(player);
                    dmzrevamp$freeze(target);
                } else if (tick == 11) {
                    Vec3 look = target.getLookAngle().normalize();
                    dmzrevamp$movePlayer(player, target.getX() - look.x * 1.5D, target.getY(), target.getZ() - look.z * 1.5D);
                    dmzrevamp$play(player, MainSounds.TP, 1.0F, 1.3F);
                } else if (tick == 17) {
                    dmzrevamp$finalBlow(player, target, hit, 3.0D, strike, data);
                    return true;
                }
            }
            case METEOR_COMBINATION -> {
                float perHit = (float) (totalDamage * 0.1D);
                float waveHit = (float) (totalDamage * 0.6D);
                if (tick < 10) {
                    double distance = player.distanceTo(target);
                    if (distance > 1.5D) {
                        Vec3 direction = target.position().subtract(player.position()).normalize();
                        FlyingStrikeYLock.allowScriptedVerticalMotion(player);
                        player.setDeltaMovement(direction.scale(1.5D));
                        player.hurtMarked = true;
                    } else {
                        dmzrevamp$freeze(player);
                    }
                    dmzrevamp$freeze(target);
                } else if (tick == 10) {
                    dmzrevamp$hit(player, target, perHit, strike, false, data);
                    dmzrevamp$play(player, MainSounds.GOLPE1, 1.5F, 1.0F);
                    dmzrevamp$spawnPunchParticles(player, target);
                    Vec3 direction = player.getLookAngle().normalize();
                    dmzrevamp$setMotion(target, direction.x * 1.5D, 0.4D, direction.z * 1.5D);
                    dmzrevamp$freeze(player);
                } else if (tick < 15) {
                    Vec3 direction = target.position().subtract(player.position()).normalize();
                    FlyingStrikeYLock.allowScriptedVerticalMotion(player);
                    player.setDeltaMovement(direction.scale(2.5D));
                    player.hurtMarked = true;
                } else if (tick == 15) {
                    dmzrevamp$hit(player, target, perHit, strike, false, data);
                    dmzrevamp$play(player, MainSounds.CRITICO2, 1.5F, 1.2F);
                    dmzrevamp$spawnPunchParticles(player, target);
                    dmzrevamp$freeze(player);
                    dmzrevamp$freeze(target);
                } else if (tick < 20) {
                    dmzrevamp$freeze(player);
                    dmzrevamp$freeze(target);
                } else if (tick == 20) {
                    dmzrevamp$hit(player, target, perHit, strike, false, data);
                    dmzrevamp$play(player, MainSounds.CRITICO2, 2.0F, 0.8F);
                    dmzrevamp$spawnPunchParticles(player, target);
                    Vec3 direction = player.getLookAngle().normalize();
                    dmzrevamp$setMotion(target, direction.x * 3.5D, 0.2D, direction.z * 3.5D);
                    dmzrevamp$freeze(player);
                } else if (tick < 34) {
                    dmzrevamp$freeze(player);
                    if (tick == 21) {
                        dmzrevamp$play(player, MainSounds.KI_EXPLOSION_CHARGE, 1.0F, 1.0F);
                    }
                } else if (tick == 34) {
                    dmzrevamp$freeze(player);
                    dmzrevamp$hit(player, target, Math.max(1.0F, waveHit * 0.1F), strike, false, data);
                    dmzrevamp$fireMeteorWave(player, Math.max(1.0F, waveHit * 0.9F));
                } else if (tick >= 50) {
                    StrikeAttackEffectApplier.applyExtras(strike, player, target);
                    return true;
                }
            }
            case FAST_PUNCH -> {
                if (tick == 1) {
                    Vec3 look = target.getLookAngle().normalize();
                    dmzrevamp$movePlayer(player, target.getX() + look.x * 1.2D, target.getY(), target.getZ() + look.z * 1.2D);
                    dmzrevamp$play(player, MainSounds.TP, 1.0F, 1.0F);
                } else if (tick == 7) {
                    dmzrevamp$hit(player, target, (float) totalDamage, strike, false, data);
                    dmzrevamp$play(player, MainSounds.CRITICO1, 1.2F, 0.7F);
                    dmzrevamp$spawnPunchParticles(player, target);
                    Vec3 dir = target.position().subtract(player.position()).normalize();
                    dmzrevamp$setMotion(target, dir.x * 5.0D, 0.5D, dir.z * 5.0D);
                } else if (tick == 13) {
                    dmzrevamp$stun(target, 60, false);
                } else if (tick >= 20) {
                    StrikeAttackEffectApplier.applyExtras(strike, player, target);
                    return true;
                }
            }
            case STRONG_PUNCH -> {
                if (tick == 1) {
                    Vec3 look = target.getLookAngle().normalize();
                    dmzrevamp$movePlayer(player, target.getX() + look.x, target.getY(), target.getZ() + look.z);
                    dmzrevamp$play(player, MainSounds.TP, 1.0F, 1.0F);
                } else if (tick == 5) {
                    dmzrevamp$hit(player, target, (float) totalDamage, strike, false, data);
                    dmzrevamp$play(player, MainSounds.CRITICO1, 1.0F, 1.2F);
                    dmzrevamp$spawnPunchParticles(player, target);
                    Vec3 dir = target.position().subtract(player.position()).normalize();
                    dmzrevamp$setMotion(target, dir.x * 2.5D, 0.3D, dir.z * 2.5D);
                } else if (tick >= 10) {
                    StrikeAttackEffectApplier.applyExtras(strike, player, target);
                    return true;
                }
            }
            default -> {
            }
        }
        return false;
    }

    @Unique
    private static void dmzrevamp$hit(ServerPlayer player, LivingEntity target, float amount, StrikeAttackData strike, boolean finalHit, StatsData data) {
        if (amount <= 0.0F || target == null || !target.isAlive()) {
            return;
        }
        target.invulnerableTime = 0;
        Object active = ACTIVE.get(player.getUUID());
        double totalDamage = active instanceof StrikeAttackActiveAccessor accessor
                ? accessor.dmzrevamp$getTotalDamage()
                : amount;
        float scaledAmount = StrikeClashManager.scaleWinningPlayerDamage(player, amount);
        double scaledTotalDamage = StrikeClashManager.scaleWinningPlayerDamage(player, totalDamage);
        boolean wasAlive = target.isAlive();
        AdaptiveDefenseDamageContext.hurt(
                target,
                MainDamageTypes.strikeAttack(player.level(), player, strike.getId()),
                scaledAmount,
                AdaptiveDefenseDamageContext.AttackType.STRIKE,
                scaledTotalDamage
        );
        int hitXp = strike.getXpGainPerHit();
        if (hitXp > 0) data.getTechniques().addExperienceToTechnique(strike.getId(), hitXp);
        if (wasAlive && !target.isAlive()) {
            int killXp = strike.getXpGainPerKill();
            if (killXp > 0) data.getTechniques().addExperienceToTechnique(strike.getId(), killXp);
        }
        if (finalHit) {
            StrikeAttackEffectApplier.applyExtras(strike, player, target);
        }
        NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
    }

    @Unique
    private static void dmzrevamp$absorbHit(ServerPlayer player, LivingEntity target, float amount, StrikeAttackData strike, StatsData data) {
        float before = target.getHealth();
        dmzrevamp$hit(player, target, amount, strike, false, data);
        float recovery = Math.max(0.0F, before - target.getHealth()) * 0.5F;
        if (recovery > 0.0F) {
            player.heal(recovery);
            data.getResources().addEnergy(recovery);
            dmzrevamp$play(player, MainSounds.ABSORB1, 0.5F, 0.8F);
            NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
        }
        dmzrevamp$spawnPunchParticles(player, target);
    }

    @Unique
    private static void dmzrevamp$teleportAndHit(ServerPlayer player, LivingEntity target, Vec3 direction, double distance, float amount, RegistryObject<SoundEvent> sound, float pitch, boolean finalHit, StrikeAttackData strike, StatsData data) {
        if (direction.lengthSqr() < 0.01D) {
            direction = player.getLookAngle();
        }
        direction = direction.normalize();
        dmzrevamp$movePlayer(player, target.getX() + direction.x * distance, target.getY(), target.getZ() + direction.z * distance);
        dmzrevamp$play(player, MainSounds.TP, 1.0F, pitch);
        dmzrevamp$hit(player, target, amount, strike, finalHit, data);
        dmzrevamp$play(player, sound, 0.8F, pitch);
        dmzrevamp$spawnPunchParticles(player, target);
    }

    @Unique
    private static void dmzrevamp$meleeHit(ServerPlayer player, LivingEntity target, float amount, double yMotion, double horizontalMotion, StrikeAttackData strike, boolean finalHit, StatsData data) {
        dmzrevamp$face(player, target);
        dmzrevamp$hit(player, target, amount, strike, finalHit, data);
        dmzrevamp$play(player, MainSounds.CRITICO1, 0.8F, 1.2F);
        dmzrevamp$spawnPunchParticles(player, target);
        if (horizontalMotion > 0.0D) {
            Vec3 dir = target.position().subtract(player.position()).normalize();
            dmzrevamp$setMotion(target, dir.x * horizontalMotion, yMotion, dir.z * horizontalMotion);
        }
    }

    @Unique
    private static void dmzrevamp$finalBlow(ServerPlayer player, LivingEntity target, float amount, double strength, StrikeAttackData strike, StatsData data) {
        dmzrevamp$hit(player, target, amount, strike, true, data);
        dmzrevamp$play(player, MainSounds.CRITICO1, 1.0F, 0.8F);
        dmzrevamp$spawnPunchParticles(player, target);
        Vec3 dir = target.position().subtract(player.position()).normalize();
        dmzrevamp$setMotion(target, dir.x * strength, 0.5D, dir.z * strength);
    }

    @Unique
    private static void dmzrevamp$fireMeteorWave(ServerPlayer player, float damage) {
        KiWaveEntity wave = new KiWaveEntity(player.level(), player);
        wave.setupKiHame(player, damage, 2.0F, 1.0F, 10);
        wave.setFiring(true);
        wave.setMaxLife(40);
        player.level().addFreshEntity(wave);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), MainSounds.KI_KAME_FIRE.get(), SoundSource.PLAYERS, 2.0F, 1.0F);
    }

    @Unique
    private static void dmzrevamp$stun(LivingEntity target, int ticks, boolean hidden) {
        target.addEffect(new MobEffectInstance(MainEffects.STUN.get(), ticks, 0, false, !hidden, true));
    }

    @Unique
    private static void dmzrevamp$spawnPunchParticles(ServerPlayer player, LivingEntity target) {
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.6D, target.getZ(), 12, 0.25D, 0.25D, 0.25D, 0.15D);
        }
    }

    @Unique
    private static void dmzrevamp$movePlayer(ServerPlayer player, double x, double y, double z) {
        player.teleportTo(x, y, z);
        FlyingStrikeYLock.updateAnchorAfterStrikeTeleport(player);
        dmzrevamp$freeze(player);
    }

    @Unique
    private static void dmzrevamp$setMotion(LivingEntity entity, double x, double y, double z) {
        entity.setDeltaMovement(x, y, z);
        entity.hurtMarked = true;
    }

    @Unique
    private static void dmzrevamp$freeze(LivingEntity entity) {
        entity.setDeltaMovement(Vec3.ZERO);
        entity.hurtMarked = true;
    }

    @Unique
    private static void dmzrevamp$face(LivingEntity entity, LivingEntity target) {
        if (entity == null || target == null) {
            return;
        }
        entity.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
        entity.setYHeadRot(entity.getYRot());
    }

    @Unique
    private static void dmzrevamp$endCustomStrike(ServerPlayer player, LivingEntity target, StatsData data) {
        Object active = ACTIVE.get(player.getUUID());
        String techniqueId = active instanceof StrikeAttackActiveAccessor accessor ? accessor.dmzrevamp$getTechniqueId() : "";
        ACTIVE.remove(player.getUUID());
        FlyingStrikeYLock.finish(player);
        if (!player.getAbilities().instabuild) {
            if (!techniqueId.isEmpty()) {
                TechniqueData unlocked = data.getTechniques().getUnlockedTechniques().get(techniqueId);
                if (unlocked instanceof StrikeAttackData strike) {
                    data.getCooldowns().setCooldown("TechniqueCooldown_" + techniqueId, strike.getActualCooldown());
                }
            } else {
                TechniqueData technique = data.getTechniques().getSelectedTechnique();
                if (technique instanceof StrikeAttackData strike) {
                    data.getCooldowns().setCooldown("TechniqueCooldown_" + strike.getId(), strike.getActualCooldown());
                }
            }
        }
        data.getStatus().setStrikeLocked(false);
        NetworkHandler.sendToTrackingEntityAndSelf(new TriggerAnimationS2C(player.getUUID(), TriggerAnimationS2C.AnimationType.KI_ANIMATION_STOP, 0, -1, ""), player);
        NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
        if (target instanceof ServerPlayer targetPlayer) {
            StatsProvider.get(StatsCapability.INSTANCE, targetPlayer).ifPresent(targetData -> {
                targetData.getStatus().setStrikeLocked(false);
                NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(targetPlayer), targetPlayer);
            });
        }
    }

    @Unique
    private static LivingEntity dmzrevamp$resolveLiving(ServerPlayer player, UUID targetId) {
        if (targetId == null) {
            return null;
        }
        net.minecraft.world.entity.Entity entity = player.serverLevel().getEntity(targetId);
        return entity instanceof LivingEntity living
                && player.getBoundingBox().inflate(64.0D).intersects(living.getBoundingBox()) ? living : null;
    }

    @Unique
    private static boolean dmzrevamp$isSelectedCustomStrike(StatsData data) {
        if (data == null) {
            return false;
        }
        TechniqueData selected = data.getTechniques().getSelectedTechnique();
        return selected instanceof StrikeAttackData strike
                && strike instanceof RevampStrikeAttackData revamp
                && revamp.dmzrevamp$isCustomStrike();
    }

    @Unique
    private static boolean dmzrevamp$isCustomStrikeCooldown(StatsData data, String cooldownKey) {
        if (data == null || cooldownKey == null || !cooldownKey.startsWith("TechniqueCooldown_")) {
            return false;
        }
        String id = cooldownKey.substring("TechniqueCooldown_".length());
        TechniqueData technique = data.getTechniques().getUnlockedTechniques().get(id);
        return technique instanceof StrikeAttackData strike
                && strike instanceof RevampStrikeAttackData revamp
                && revamp.dmzrevamp$isCustomStrike();
    }

    @Unique
    private static String dmzrevamp$pendingCooldownKey(Object pendingStrike) {
        String id = pendingStrike instanceof StrikeAttackPendingAccessor accessor ? accessor.dmzrevamp$getTechniqueId() : "";
        return id.isEmpty() ? "" : "TechniqueCooldown_" + id;
    }

    @Unique
    private static void dmzrevamp$lockCustomStrikeTarget(ServerPlayer player, LivingEntity target, StatsData data) {
        data.getStatus().setStrikeLocked(true);
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
        target.setDeltaMovement(Vec3.ZERO);
        target.hurtMarked = true;
        if (target instanceof ServerPlayer targetPlayer) {
            StatsProvider.get(StatsCapability.INSTANCE, targetPlayer).ifPresent(targetData -> {
                targetData.getStatus().setStrikeLocked(true);
                NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(targetPlayer), targetPlayer);
            });
        }
    }

    @Unique
    private static void dmzrevamp$pushNearby(ServerPlayer player) {
        int tick = player.tickCount;
        Integer previous = DMZREVAMP_EVASIVE_PUSH_TICK.get(player.getUUID());
        if (previous != null && previous == tick) {
            return;
        }
        DMZREVAMP_EVASIVE_PUSH_TICK.put(player.getUUID(), tick);
        dmzrevamp$play(player, MainSounds.EVASION1, 1.0F, 1.0F);
        AABB area = player.getBoundingBox().inflate(4.0D);
        for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, area, entity -> entity != player && entity.isAlive())) {
            Vec3 direction = entity.position().subtract(player.position());
            if (direction.lengthSqr() < 0.01D) {
                direction = player.getLookAngle();
            }
            direction = direction.normalize();
            entity.setDeltaMovement(direction.x * 1.4D, 0.35D, direction.z * 1.4D);
            entity.hurtMarked = true;
        }
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.5D, player.getZ(), 16, 1.0D, 0.35D, 1.0D, 0.08D);
        }
    }

    @Unique
    private static void dmzrevamp$pushTarget(ServerPlayer player, LivingEntity target) {
        dmzrevamp$play(player, MainSounds.FIST_PUNCH, 1.2F, 1.0F);
        Vec3 direction = target.position().subtract(player.position());
        if (direction.lengthSqr() < 0.01D) {
            direction = player.getLookAngle();
        }
        direction = direction.normalize();
        target.setDeltaMovement(direction.x * 3.5D, 0.45D, direction.z * 3.5D);
        target.hurtMarked = true;
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(), 12, 0.35D, 0.25D, 0.35D, 0.08D);
        }
    }

    @Unique
    private static void dmzrevamp$play(ServerPlayer player, RegistryObject<SoundEvent> sound, float volume, float pitch) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound.get(), player.getSoundSource(), volume, pitch);
    }
}
