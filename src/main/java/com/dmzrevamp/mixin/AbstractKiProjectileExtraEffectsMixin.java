package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.ki.KiAttackExtraEffectApplier;
import com.dmzrevamp.revamp.ki.KiAttackMobSpeedDebuffs;
import com.dmzrevamp.revamp.ki.RevampKiAttackData;
import com.dragonminez.common.combat.logic.player.TargetHelper;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.dragonminez.common.init.entities.ki.KiAreaEntity;
import com.dragonminez.common.init.entities.ki.KiBlastEntity;
import com.dragonminez.common.init.entities.ki.OzaruFistEntity;
import com.dragonminez.common.init.entities.ki.SPDragonFistEntity;
import com.dragonminez.common.init.MainDamageTypes;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.passives.ClassPassives;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.EntityStatDebuffs;
import com.dragonminez.common.stats.character.SecondaryStatEffects;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.techniques.TechniqueData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractKiProjectile.class)
public abstract class AbstractKiProjectileExtraEffectsMixin {
    private static final String PROJECTILES_SPAWNED_TAG = "dmzrevamp_projectiles_spawned";
    private static final String PROJECTILE_DELAY_TAG = "dmzrevamp_projectile_delay";
    private static final String PROJECTILE_DIR_X_TAG = "dmzrevamp_projectile_dir_x";
    private static final String PROJECTILE_DIR_Y_TAG = "dmzrevamp_projectile_dir_y";
    private static final String PROJECTILE_DIR_Z_TAG = "dmzrevamp_projectile_dir_z";
    private static final String PROJECTILE_SPEED_TAG = "dmzrevamp_projectile_speed";
    private static final String PROJECTILE_POS_X_TAG = "dmzrevamp_projectile_pos_x";
    private static final String PROJECTILE_POS_Y_TAG = "dmzrevamp_projectile_pos_y";
    private static final String PROJECTILE_POS_Z_TAG = "dmzrevamp_projectile_pos_z";
    private static final String PROJECTILE_QUEUE_COUNT_TAG = "dmzrevamp_projectile_queue_count";
    private static final String PROJECTILE_QUEUE_INDEX_TAG = "dmzrevamp_projectile_queue_index";
    private static final String PROJECTILE_QUEUE_DELAY_TAG = "dmzrevamp_projectile_queue_delay";

    @Inject(method = "applyDamageOrHeal", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzrevamp$overrideSpecialArchetypeHit(Entity target, float amount, CallbackInfoReturnable<Boolean> cir) {
        AbstractKiProjectile projectile = (AbstractKiProjectile) (Object) this;
        if (projectile instanceof SPDragonFistEntity || projectile instanceof OzaruFistEntity) {
            cir.setReturnValue(dmzrevamp$applyStrikeProjectileDamage(projectile, target, amount));
            return;
        }

        RevampKiAttackData revamp = dmzrevamp$revamp(projectile);
        if (revamp != null && revamp.dmzrevamp$isAreaBothUtility() && projectile instanceof KiAreaEntity) {
            cir.setReturnValue(dmzrevamp$applyAreaBoth(projectile, target, amount));
        }
    }

    @Inject(method = "applyDamageOrHeal", at = @At("RETURN"), remap = false)
    private void dmzrevamp$applyExtraPotionEffects(Entity target, float amount, CallbackInfoReturnable<Boolean> cir) {
        AbstractKiProjectile projectile = (AbstractKiProjectile) (Object) this;
        if (projectile instanceof SPDragonFistEntity || projectile instanceof OzaruFistEntity) {
            return;
        }
        if (projectile instanceof KiAreaEntity && !projectile.isFiring()) {
            return;
        }

        RevampKiAttackData revamp = dmzrevamp$revamp((AbstractKiProjectile) (Object) this);
        if (revamp != null && revamp.dmzrevamp$isAreaBothUtility()) {
            return;
        }
        if (projectile instanceof KiAreaEntity && projectile.isHeal() && dmzrevamp$isFriendlyToOwner(projectile, target)) {
            dmzrevamp$applyFriendlyAreaBenefits(projectile, target);
            return;
        }
        if (cir.getReturnValueZ()) {
            KiAttackExtraEffectApplier.apply(projectile, target);
            dmzrevamp$applyMobSpdDebuffs(projectile, target);
        }
    }

    @Inject(method = "onSuccessfulHit", at = @At("RETURN"), remap = false)
    private void dmzrevamp$applyAdditionalTechniqueEffects(Entity target, CallbackInfo ci) {
        AbstractKiProjectile projectile = (AbstractKiProjectile) (Object) this;
        if (projectile instanceof SPDragonFistEntity || projectile instanceof OzaruFistEntity) {
            return;
        }
        if (projectile instanceof KiAreaEntity && !projectile.isFiring()) {
            return;
        }
        RevampKiAttackData revamp = dmzrevamp$revamp(projectile);
        if (revamp != null && revamp.dmzrevamp$isAreaBothUtility()) {
            return;
        }
        dmzrevamp$applyAdditionalStatEffects(projectile, target);
    }

    @Inject(method = "shouldDamage", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzrevamp$allowAreaBothTargets(Entity target, CallbackInfoReturnable<Boolean> cir) {
        AbstractKiProjectile projectile = (AbstractKiProjectile) (Object) this;
        RevampKiAttackData revamp = dmzrevamp$revamp(projectile);
        if (revamp != null && revamp.dmzrevamp$isAreaBothUtility() && projectile instanceof KiAreaEntity && target instanceof LivingEntity) {
            cir.setReturnValue(target != projectile);
        }
    }

    @Inject(method = "m_8119_", at = @At("TAIL"), remap = false)
    private void dmzrevamp$tickArchetypeProjectile(CallbackInfo ci) {
        AbstractKiProjectile projectile = (AbstractKiProjectile) (Object) this;
        dmzrevamp$tickDelayedProjectile(projectile);
        if (projectile.isFiring() && projectile instanceof KiBlastEntity blast) {
            dmzrevamp$spawnMediumBallProjectiles(blast);
        }
    }

    @Unique
    private boolean dmzrevamp$applyStrikeProjectileDamage(AbstractKiProjectile projectile, Entity target, float amount) {
        if (!(target instanceof LivingEntity livingTarget) || projectile.level().isClientSide()) {
            return false;
        }
        Entity owner = projectile.getOwner();
        if (owner == null) {
            return false;
        }
        String strikeId = projectile instanceof SPDragonFistEntity ? "dragon_fist" : "oozaru_fist";
        return livingTarget.hurt(MainDamageTypes.strikeAttack(projectile.level(), owner, strikeId), amount);
    }

    @Unique
    private boolean dmzrevamp$applyAreaBoth(AbstractKiProjectile projectile, Entity target, float amount) {
        if (!(target instanceof LivingEntity livingTarget)) {
            return false;
        }
        if (!projectile.isFiring()) {
            return false;
        }
        boolean friendly = dmzrevamp$isFriendlyToOwner(projectile, target);
        boolean applied;
        if (friendly) {
            applied = dmzrevamp$applyFriendlyAreaBenefits(projectile, target);
            if (livingTarget.getHealth() >= livingTarget.getMaxHealth()) {
                // Existing health does not block short-lived technique buffs.
            } else {
                livingTarget.heal(amount);
                applied = true;
            }
        } else if (!projectile.level().isClientSide()) {
            applied = livingTarget.hurt(MainDamageTypes.kiblast(projectile.level(), projectile, projectile.getOwner()), amount);
        } else {
            applied = false;
        }
        if (!friendly && applied) {
            KiAttackExtraEffectApplier.applyAreaBothExtras(projectile, target, friendly);
        }
        return applied;
    }

    @Unique
    private boolean dmzrevamp$applyFriendlyAreaBenefits(AbstractKiProjectile projectile, Entity target) {
        if (!(target instanceof LivingEntity)) {
            return false;
        }
        if (!projectile.isFiring()) {
            return false;
        }
        projectile.applyTechniqueSecondaryEffect(target);
        boolean applied = dmzrevamp$hasBeneficialPrimarySecondary(projectile);
        applied |= dmzrevamp$applyAdditionalStatEffects(projectile, target);
        applied |= KiAttackExtraEffectApplier.applyAreaBothExtras(projectile, target, true);
        return applied;
    }

    @Unique
    private boolean dmzrevamp$hasBeneficialPrimarySecondary(AbstractKiProjectile projectile) {
        KiAttackData technique = dmzrevamp$technique(projectile);
        return technique != null
                && technique.hasValidSecondaryEffect()
                && technique.getSecondaryEffectType() == KiAttackData.SecondaryEffectType.BUFF;
    }

    @Unique
    private boolean dmzrevamp$applyAdditionalStatEffects(AbstractKiProjectile projectile, Entity target) {
        if (!(target instanceof LivingEntity livingTarget)) {
            return false;
        }
        RevampKiAttackData revamp = dmzrevamp$revamp(projectile);
        if (revamp == null) {
            return false;
        }
        KiAttackData technique = dmzrevamp$technique(projectile);
        if (technique == null) {
            return false;
        }
        StatsData ownerData = projectile.getOwner() instanceof LivingEntity owner
                ? StatsProvider.get(StatsCapability.INSTANCE, owner).resolve().orElse(null)
                : null;
        double durationMultiplier = ownerData == null ? 1.0D : ClassPassives.get(ownerData).secondaryDurationMultiplier(ownerData, technique);
        boolean applied = false;
        applied |= dmzrevamp$applyAdditionalStatEffect(projectile, livingTarget, revamp.dmzrevamp$getThirdEffectType(), revamp.dmzrevamp$getThirdAffectedStat(),
                revamp.dmzrevamp$getThirdIntensity(), revamp.dmzrevamp$getThirdDuration(), durationMultiplier);
        applied |= dmzrevamp$applyAdditionalStatEffect(projectile, livingTarget, revamp.dmzrevamp$getFourthEffectType(), revamp.dmzrevamp$getFourthAffectedStat(),
                revamp.dmzrevamp$getFourthIntensity(), revamp.dmzrevamp$getFourthDuration(), durationMultiplier);
        return applied;
    }

    @Unique
    private boolean dmzrevamp$applyAdditionalStatEffect(AbstractKiProjectile projectile, LivingEntity target,
                                                       KiAttackData.SecondaryEffectType type, KiAttackData.AffectedStat stat,
                                                       float intensity, int durationSeconds, double durationMultiplier) {
        if (type == KiAttackData.SecondaryEffectType.NONE || stat == null || intensity <= 0.0F || durationSeconds <= 0) {
            return false;
        }
        boolean buff = type == KiAttackData.SecondaryEffectType.BUFF;
        if (!dmzrevamp$secondaryRelationAllows(projectile, target, buff)) {
            return false;
        }
        double modifier = intensity / 100.0D;
        if (!buff) {
            modifier = -modifier;
        }
        int durationTicks = Math.max(1, (int) Math.round(durationSeconds * 20.0D * durationMultiplier));
        String statName = stat.name();

        StatsData targetData = StatsProvider.get(StatsCapability.INSTANCE, target).resolve().orElse(null);
        if (targetData != null) {
            SecondaryStatEffects effects = targetData.getSecondaryStatEffects();
            effects.apply(statName, modifier, durationTicks);
            if (target instanceof ServerPlayer player) {
                NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
            }
            return true;
        }

        if (!buff && EntityStatDebuffs.isSupported(statName)) {
            EntityStatDebuffs.applyDebuff(target, statName, modifier, durationTicks);
            return true;
        }
        return false;
    }

    @Unique
    private boolean dmzrevamp$secondaryRelationAllows(AbstractKiProjectile projectile, Entity target, boolean buff) {
        Entity owner = projectile.getOwner();
        if (target == owner) {
            return buff;
        }
        boolean friendly = dmzrevamp$isFriendlyToOwner(projectile, target);
        if (buff) {
            return friendly;
        }
        return !friendly;
    }

    @Unique
    private void dmzrevamp$applyMobSpdDebuffs(AbstractKiProjectile projectile, Entity target) {
        if (!(target instanceof Mob mob)) {
            return;
        }
        KiAttackData technique = dmzrevamp$technique(projectile);
        if (technique == null) {
            return;
        }
        double durationMultiplier = 1.0D;
        if (projectile.getOwner() instanceof LivingEntity owner) {
            StatsData ownerData = StatsProvider.get(StatsCapability.INSTANCE, owner).resolve().orElse(null);
            if (ownerData != null) {
                durationMultiplier = ClassPassives.get(ownerData).secondaryDurationMultiplier(ownerData, technique);
            }
        }
        dmzrevamp$applyMobSpdDebuff(mob, technique.getSecondaryEffectType(), technique.getAffectedStat(),
                technique.getSecondaryIntensity(), technique.getSecondaryDuration(), durationMultiplier);
        RevampKiAttackData revamp = dmzrevamp$revamp(projectile);
        if (revamp == null) {
            return;
        }
        dmzrevamp$applyMobSpdDebuff(mob, revamp.dmzrevamp$getThirdEffectType(), revamp.dmzrevamp$getThirdAffectedStat(),
                revamp.dmzrevamp$getThirdIntensity(), revamp.dmzrevamp$getThirdDuration(), durationMultiplier);
        dmzrevamp$applyMobSpdDebuff(mob, revamp.dmzrevamp$getFourthEffectType(), revamp.dmzrevamp$getFourthAffectedStat(),
                revamp.dmzrevamp$getFourthIntensity(), revamp.dmzrevamp$getFourthDuration(), durationMultiplier);
    }

    @Unique
    private void dmzrevamp$applyMobSpdDebuff(Mob mob, KiAttackData.SecondaryEffectType type, KiAttackData.AffectedStat stat,
                                             float intensity, int durationSeconds, double durationMultiplier) {
        if (type != KiAttackData.SecondaryEffectType.DEBUFF || stat != KiAttackData.AffectedStat.SKP
                || intensity <= 0.0F || durationSeconds <= 0) {
            return;
        }
        int durationTicks = Math.max(1, (int) Math.round(durationSeconds * 20.0D * durationMultiplier));
        KiAttackMobSpeedDebuffs.apply(mob, -(intensity / 100.0D), durationTicks);
    }

    @Unique
    private void dmzrevamp$tickDelayedProjectile(AbstractKiProjectile projectile) {
        CompoundTag tag = projectile.getPersistentData();
        if (!tag.contains(PROJECTILE_DELAY_TAG)) {
            return;
        }
        int delay = tag.getInt(PROJECTILE_DELAY_TAG);
        if (delay > 0) {
            tag.putInt(PROJECTILE_DELAY_TAG, delay - 1);
            projectile.setDeltaMovement(Vec3.ZERO);
            projectile.setPos(tag.getDouble(PROJECTILE_POS_X_TAG), tag.getDouble(PROJECTILE_POS_Y_TAG), tag.getDouble(PROJECTILE_POS_Z_TAG));
            return;
        }
        tag.remove(PROJECTILE_DELAY_TAG);
        Vec3 direction = new Vec3(tag.getDouble(PROJECTILE_DIR_X_TAG), tag.getDouble(PROJECTILE_DIR_Y_TAG), tag.getDouble(PROJECTILE_DIR_Z_TAG));
        double speed = Math.max(0.08D, tag.getDouble(PROJECTILE_SPEED_TAG));
        Vec3 spawn = new Vec3(tag.getDouble(PROJECTILE_POS_X_TAG), tag.getDouble(PROJECTILE_POS_Y_TAG), tag.getDouble(PROJECTILE_POS_Z_TAG));
        if (direction.lengthSqr() <= 0.01D) {
            direction = projectile.getOwner() instanceof LivingEntity owner ? owner.getLookAngle() : new Vec3(0.0D, 0.0D, 1.0D);
        }
        if (projectile instanceof KiBlastEntity blast) {
            blast.fireHability(Math.max(20, projectile.getMaxLife()));
        } else {
            projectile.setFiring(true);
        }
        projectile.setPos(spawn.x, spawn.y, spawn.z);
        projectile.setDeltaMovement(direction.normalize().scale(speed));
    }

    @Unique
    private void dmzrevamp$spawnMediumBallProjectiles(KiBlastEntity original) {
        CompoundTag tag = original.getPersistentData();
        if (tag.getBoolean(PROJECTILES_SPAWNED_TAG) || !(original.getOwner() instanceof LivingEntity owner)) {
            return;
        }
        if (!(original.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        KiAttackData technique = dmzrevamp$technique(original);
        if (technique == null
                || (technique.getKiType() != KiAttackData.KiType.MEDIUM_BALL && technique.getKiType() != KiAttackData.KiType.SMALL_BALL)
                || dmzrevamp$revamp(original) == null) {
            return;
        }
        int count = Math.max(1, Math.min(dmzrevamp$maxProjectiles(technique), dmzrevamp$revamp(original).dmzrevamp$getMultiCastCount()));
        if (count <= 1) {
            tag.putBoolean(PROJECTILES_SPAWNED_TAG, true);
            return;
        }
        float splitDamage = original.getKiDamage() / count;
        original.setKiDamage(splitDamage);
        tag.putBoolean(PROJECTILES_SPAWNED_TAG, true);

        Vec3 baseDirection = original.getDeltaMovement().normalize();
        if (baseDirection.lengthSqr() <= 0.01D) {
            baseDirection = owner.getLookAngle().normalize();
        }
        double speed = original.getDeltaMovement().length();
        if (speed <= 0.01D) {
            speed = Math.max(0.08D, original.getKiSpeed());
        }
        Vec3 originalOffset = dmzrevamp$projectileArcOffset(owner, 0, count);
        original.setPos(owner.getX() + originalOffset.x, owner.getEyeY() - 0.1D + originalOffset.y, owner.getZ() + originalOffset.z);
        original.setDeltaMovement(baseDirection.scale(speed));
        if (technique.getKiType() == KiAttackData.KiType.SMALL_BALL) {
            for (int index = 1; index < count; index++) {
                dmzrevamp$spawnProjectileClone(original, owner, baseDirection, speed, index, count, 0);
            }
            return;
        }
        for (int index = 1; index < count; index++) {
            dmzrevamp$spawnProjectileClone(original, owner, baseDirection, speed, index, count, 0);
        }
    }

    @Unique
    private void dmzrevamp$tickQueuedMediumBallProjectiles(KiBlastEntity original) {
        CompoundTag tag = original.getPersistentData();
        if (!tag.contains(PROJECTILE_QUEUE_COUNT_TAG) || !(original.getOwner() instanceof LivingEntity owner)) {
            return;
        }
        int count = tag.getInt(PROJECTILE_QUEUE_COUNT_TAG);
        int index = tag.getInt(PROJECTILE_QUEUE_INDEX_TAG);
        if (index >= count) {
            tag.remove(PROJECTILE_QUEUE_COUNT_TAG);
            tag.remove(PROJECTILE_QUEUE_INDEX_TAG);
            tag.remove(PROJECTILE_QUEUE_DELAY_TAG);
            return;
        }
        int delay = tag.getInt(PROJECTILE_QUEUE_DELAY_TAG);
        if (delay > 0) {
            tag.putInt(PROJECTILE_QUEUE_DELAY_TAG, delay - 1);
            return;
        }
        Vec3 direction = new Vec3(tag.getDouble(PROJECTILE_DIR_X_TAG), tag.getDouble(PROJECTILE_DIR_Y_TAG), tag.getDouble(PROJECTILE_DIR_Z_TAG));
        if (direction.lengthSqr() <= 0.01D) {
            direction = owner.getLookAngle();
        }
        double speed = Math.max(0.08D, tag.getDouble(PROJECTILE_SPEED_TAG));
        dmzrevamp$spawnProjectileClone(original, owner, direction, speed, index, count, 0);
        tag.putInt(PROJECTILE_QUEUE_INDEX_TAG, index + 1);
        tag.putInt(PROJECTILE_QUEUE_DELAY_TAG, 5);
    }

    @Unique
    private void dmzrevamp$spawnProjectileClone(KiBlastEntity original, LivingEntity owner, Vec3 direction, double speed, int index, int count, int delayTicks) {
        Vec3 offset = dmzrevamp$projectileArcOffset(owner, index, count);
        KiBlastEntity clone = new KiBlastEntity(original.level(), owner);
        clone.setupKiBlastPlayer(owner, original.getKiDamage(), original.getKiSpeed(), original.getColor(), original.getColorBorder(), original.getColorOutline(), original.getSize());
        clone.setTechniqueId(original.getTechniqueId());
        clone.setArmorPenetration(original.getArmorPenetration());
        clone.setHeal(original.isHeal());
        clone.setMaxLife(original.getMaxLife());
        clone.setHomingTarget(dmzrevamp$nativeHomingTargetId(owner));
        clone.setPos(owner.getX() + offset.x, owner.getEyeY() - 0.1D + offset.y, owner.getZ() + offset.z);
        CompoundTag cloneTag = clone.getPersistentData();
        cloneTag.putBoolean(PROJECTILES_SPAWNED_TAG, true);
        if (delayTicks > 0) {
            Vec3 spawn = new Vec3(owner.getX() + offset.x, owner.getEyeY() - 0.1D + offset.y, owner.getZ() + offset.z);
            clone.setFiring(false);
            clone.setDeltaMovement(Vec3.ZERO);
            cloneTag.putInt(PROJECTILE_DELAY_TAG, delayTicks);
            cloneTag.putDouble(PROJECTILE_DIR_X_TAG, direction.x);
            cloneTag.putDouble(PROJECTILE_DIR_Y_TAG, direction.y);
            cloneTag.putDouble(PROJECTILE_DIR_Z_TAG, direction.z);
            cloneTag.putDouble(PROJECTILE_SPEED_TAG, speed);
            cloneTag.putDouble(PROJECTILE_POS_X_TAG, spawn.x);
            cloneTag.putDouble(PROJECTILE_POS_Y_TAG, spawn.y);
            cloneTag.putDouble(PROJECTILE_POS_Z_TAG, spawn.z);
        } else {
            clone.setFiring(true);
            clone.setDeltaMovement(direction.normalize().scale(speed));
        }
        original.level().addFreshEntity(clone);
    }

    @Unique
    private KiAttackData dmzrevamp$technique(AbstractKiProjectile projectile) {
        if (!(projectile.getOwner() instanceof ServerPlayer owner)) {
            return null;
        }
        StatsData ownerData = StatsProvider.get(StatsCapability.INSTANCE, owner).resolve().orElse(null);
        if (ownerData == null) {
            return null;
        }
        TechniqueData technique = ownerData.getTechniques().getUnlockedTechniques().get(projectile.getTechniqueId());
        return technique instanceof KiAttackData ki ? ki : null;
    }

    @Unique
    private RevampKiAttackData dmzrevamp$revamp(AbstractKiProjectile projectile) {
        KiAttackData technique = dmzrevamp$technique(projectile);
        return technique instanceof RevampKiAttackData revamp ? revamp : null;
    }

    @Unique
    private boolean dmzrevamp$isFriendlyToOwner(AbstractKiProjectile projectile, Entity target) {
        Entity owner = projectile.getOwner();
        if (owner == target) {
            return true;
        }
        return owner instanceof Player playerOwner
                && TargetHelper.getRelation(playerOwner, target) == TargetHelper.Relation.FRIENDLY;
    }

    @Unique
    private int dmzrevamp$maxProjectiles(KiAttackData technique) {
        return technique.getKiType() == KiAttackData.KiType.SMALL_BALL ? 10 : 5;
    }

    @Unique
    private int dmzrevamp$nativeHomingTargetId(LivingEntity owner) {
        StatsData data = StatsProvider.get(StatsCapability.INSTANCE, owner).resolve().orElse(null);
        return data == null ? -1 : data.getTechniques().getHomingTargetId();
    }

    @Unique
    private Vec3 dmzrevamp$projectileArcOffset(LivingEntity owner, int index, int count) {
        if (count <= 1) {
            return owner.getLookAngle().normalize().scale(1.0D);
        }
        Vec3 look = owner.getLookAngle().normalize();
        Vec3 right = new Vec3(-look.z, 0.0D, look.x);
        if (right.lengthSqr() <= 0.01D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }
        double t = count == 1 ? 0.0D : -1.0D + (2.0D * index / (count - 1.0D));
        double horizontal = t * Math.min(1.7D, 0.45D + count * 0.13D);
        double vertical = Math.max(0.0D, 1.0D - Math.abs(t)) * Math.min(1.2D, 0.35D + count * 0.08D);
        return look.scale(1.0D).add(right.scale(horizontal)).add(0.0D, vertical, 0.0D);
    }
}
