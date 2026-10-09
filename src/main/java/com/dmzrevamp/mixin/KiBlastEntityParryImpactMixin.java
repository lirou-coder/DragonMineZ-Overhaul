package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.ki.KiParryReflector;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.dragonminez.common.init.entities.ki.KiBlastEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Stops the current collision path before a successfully reflected blast detonates. */
@Mixin(value = KiBlastEntity.class, remap = false)
public abstract class KiBlastEntityParryImpactMixin {
    @Inject(
        method = {"onHitEntity", "m_5790_"},
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dragonminez/common/init/entities/ki/AbstractKiProjectile;applyDamageOrHeal(Lnet/minecraft/world/entity/Entity;F)Z",
                    shift = At.Shift.AFTER
            ),
            cancellable = true,
            require = 0,
            remap = false
    )
    private void dmzrevamp$keepReflectedBlastFlying(EntityHitResult hit, CallbackInfo ci) {
        if (KiParryReflector.consumeReflectedBlastImpact((KiBlastEntity) (Object) this)) ci.cancel();
    }

    @Inject(
        method = {"onHitEntity", "m_5790_"},
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dragonminez/common/init/entities/ki/KiBlastEntity;applyDamageOrHeal(Lnet/minecraft/world/entity/Entity;F)Z",
                    shift = At.Shift.AFTER
            ),
            cancellable = true,
            require = 0,
            remap = false
    )
    private void dmzrevamp$keepReflectedBlastFlyingFromConcreteCall(EntityHitResult hit, CallbackInfo ci) {
        if (KiParryReflector.consumeReflectedBlastImpact((KiBlastEntity) (Object) this)) ci.cancel();
    }
}
