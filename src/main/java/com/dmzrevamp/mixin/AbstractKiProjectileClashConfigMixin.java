package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.ki.KiClashAttackResolver;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractKiProjectile.class)
public abstract class AbstractKiProjectileClashConfigMixin {
    @Inject(method = "getClashRole", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzrevamp$configuredClashRole(CallbackInfoReturnable<AbstractKiProjectile.ClashRole> cir) {
        AbstractKiProjectile projectile = (AbstractKiProjectile) (Object) this;
        cir.setReturnValue(KiClashAttackResolver.isAllowed(projectile)
                ? AbstractKiProjectile.ClashRole.MAJOR : AbstractKiProjectile.ClashRole.NONE);
    }

    @Inject(method = "isClashableBeam", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzrevamp$configuredClashable(CallbackInfoReturnable<Boolean> cir) {
        AbstractKiProjectile projectile = (AbstractKiProjectile) (Object) this;
        cir.setReturnValue(KiClashAttackResolver.isAllowed(projectile) && KiClashAttackResolver.isLaunched(projectile));
    }

    @Inject(method = "setFiring", at = @At("HEAD"), remap = false)
    private void dmzrevamp$rememberLaunch(boolean firing, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (firing) ((AbstractKiProjectile) (Object) this).getPersistentData().putBoolean(KiClashAttackResolver.LAUNCHED_TAG, true);
    }
}
