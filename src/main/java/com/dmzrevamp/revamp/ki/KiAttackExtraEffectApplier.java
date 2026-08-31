package com.dmzrevamp.revamp.ki;

import com.dmzrevamp.revamp.classes.skills.ClassSkillEvents;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

public final class KiAttackExtraEffectApplier {
    private KiAttackExtraEffectApplier() {
    }

    public static void apply(AbstractKiProjectile projectile, Entity target) {
        if (!(target instanceof LivingEntity livingTarget)) {
            return;
        }

        RevampKiAttackData revamp = KiAttackRuntimeHelper.revamp(projectile);
        if (revamp == null) {
            return;
        }

        double durationMultiplier = durationMultiplier(projectile);
        applyExtra(revamp.dmzrevamp$getExtraEffectOne(), projectile.isHeal(), livingTarget, durationMultiplier);
        applyExtra(revamp.dmzrevamp$getExtraEffectTwo(), projectile.isHeal(), livingTarget, durationMultiplier);
    }

    public static boolean applyDomain(AbstractKiProjectile projectile, Entity target) {
        if (!(target instanceof LivingEntity livingTarget)) {
            return false;
        }
        RevampKiAttackData revamp = KiAttackRuntimeHelper.revamp(projectile);
        if (revamp == null) {
            return false;
        }
        boolean owner = KiAttackRuntimeHelper.isOwner(projectile, target);
        boolean applied = false;
        double durationMultiplier = durationMultiplier(projectile);
        applied |= applyDomainExtra(revamp.dmzrevamp$getExtraEffectOne(), owner, livingTarget, durationMultiplier);
        applied |= applyDomainExtra(revamp.dmzrevamp$getExtraEffectTwo(), owner, livingTarget, durationMultiplier);
        return applied;
    }

    public static boolean applyAreaBothExtras(AbstractKiProjectile projectile, Entity target, boolean friendly) {
        if (!(target instanceof LivingEntity livingTarget)) {
            return false;
        }
        RevampKiAttackData revamp = KiAttackRuntimeHelper.revamp(projectile);
        if (revamp == null) {
            return false;
        }
        boolean applied = false;
        double durationMultiplier = durationMultiplier(projectile);
        applied |= applyDomainExtra(revamp.dmzrevamp$getExtraEffectOne(), friendly, livingTarget, durationMultiplier);
        applied |= applyDomainExtra(revamp.dmzrevamp$getExtraEffectTwo(), friendly, livingTarget, durationMultiplier);
        return applied;
    }

    private static void applyExtra(KiAttackExtraEffect extra, boolean healProjectile, LivingEntity target, double durationMultiplier) {
        if (!extra.isActive()) {
            return;
        }
        if (healProjectile && extra.mode() != KiAttackExtraEffect.Mode.BENEFICIAL) {
            return;
        }
        if (!healProjectile && extra.mode() != KiAttackExtraEffect.Mode.HARMFUL) {
            return;
        }

        ResourceLocation id = ResourceLocation.tryParse(extra.effectId());
        if (!KiAttackExtraEffectRules.isAllowed(id)) {
            return;
        }

        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(id);
        if (effect == null) {
            return;
        }
        if (extra.mode() == KiAttackExtraEffect.Mode.HARMFUL && effect.isBeneficial()) {
            return;
        }
        if (extra.mode() == KiAttackExtraEffect.Mode.BENEFICIAL && !effect.isBeneficial()) {
            return;
        }

        int durationTicks = KiAttackExtraEffectRules.clampAppliedDurationTicks(extra.mode(), scaledDurationTicks(extra.durationSeconds(), durationMultiplier), target instanceof Player);
        refreshDurationEffect(target, effect, durationTicks, Math.max(0, extra.level() - 1));
    }

    private static boolean applyDomainExtra(KiAttackExtraEffect extra, boolean owner, LivingEntity target, double durationMultiplier) {
        if (!extra.isActive()) {
            return false;
        }
        if (owner && extra.mode() != KiAttackExtraEffect.Mode.BENEFICIAL) {
            return false;
        }
        if (!owner && extra.mode() != KiAttackExtraEffect.Mode.HARMFUL) {
            return false;
        }
        ResourceLocation id = ResourceLocation.tryParse(extra.effectId());
        if (!KiAttackExtraEffectRules.isAllowed(id)) {
            return false;
        }
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(id);
        if (effect == null) {
            return false;
        }
        if (extra.mode() == KiAttackExtraEffect.Mode.HARMFUL && effect.isBeneficial()) {
            return false;
        }
        if (extra.mode() == KiAttackExtraEffect.Mode.BENEFICIAL && !effect.isBeneficial()) {
            return false;
        }
        int durationTicks = KiAttackExtraEffectRules.clampAppliedDurationTicks(extra.mode(), scaledDurationTicks(extra.durationSeconds(), durationMultiplier), target instanceof Player);
        refreshDurationEffect(target, effect, durationTicks, Math.max(0, extra.level() - 1));
        return true;
    }

    private static int scaledDurationTicks(int durationSeconds, double durationMultiplier) {
        return Math.max(1, (int) Math.round(durationSeconds * 20D * Math.max(0D, durationMultiplier)));
    }

    private static double durationMultiplier(AbstractKiProjectile projectile) {
        if (!(projectile.getOwner() instanceof LivingEntity owner)) {
            return 1D;
        }
        StatsData data = StatsProvider.get(StatsCapability.INSTANCE, owner).resolve().orElse(null);
        return data == null ? 1D : ClassSkillEvents.kiEffectDurationMultiplier(data, KiAttackRuntimeHelper.technique(projectile));
    }

    private static void refreshDurationEffect(LivingEntity target, MobEffect effect, int durationTicks, int amplifier) {
        MobEffectInstance current = target.getEffect(effect);
        if (current != null && current.getAmplifier() > amplifier && current.getDuration() >= durationTicks) {
            return;
        }
        target.addEffect(new MobEffectInstance(effect, durationTicks, amplifier));
    }
}
