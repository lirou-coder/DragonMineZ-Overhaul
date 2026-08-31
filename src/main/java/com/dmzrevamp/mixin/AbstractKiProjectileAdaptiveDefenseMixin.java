package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.combat.AdaptiveDefenseDamageContext;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = AbstractKiProjectile.class, remap = false)
public abstract class AbstractKiProjectileAdaptiveDefenseMixin {
    @Shadow
    public abstract float getKiDamage();

    @Shadow
    public abstract int getMaxHits();

    @Redirect(
            method = "applyDamageOrHeal",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;m_6469_(Lnet/minecraft/world/damagesource/DamageSource;F)Z"
            ),
            remap = false,
            require = 1
    )
    private boolean dmzrevamp$useWholeKiTechniqueForAdaptiveDefense(
            LivingEntity target,
            DamageSource source,
            float modifiedHitDamage,
            Entity originalTarget,
            float originalHitDamage
    ) {
        double totalDamage;
        if (originalHitDamage > 0F && Float.isFinite(originalHitDamage)) {
            totalDamage = getKiDamage() * (modifiedHitDamage / (double) originalHitDamage);
        } else {
            totalDamage = modifiedHitDamage * Math.max(1, getMaxHits());
        }
        return AdaptiveDefenseDamageContext.hurt(
                target,
                source,
                modifiedHitDamage,
                AdaptiveDefenseDamageContext.AttackType.KI,
                totalDamage
        );
    }
}
