package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.ki.KiAttackOverhaul;
import com.dmzrevamp.revamp.ki.KiAttackOverhaulEvents;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbstractKiProjectile.class, priority = 500)
public abstract class AbstractKiProjectileOverchargeDestructionMixin {
    @Inject(method = "getDestructionMultiplier", at = @At("RETURN"), cancellable = true, remap = false)
    private void dmzrevamp$applyOverchargeDestruction(CallbackInfoReturnable<Double> cir) {
        AbstractKiProjectile projectile = (AbstractKiProjectile) (Object) this;
        float chargePercent = projectile.getPersistentData().getFloat(KiAttackOverhaulEvents.OVERCHARGE_PERCENT_TAG);
        float multiplier = KiAttackOverhaul.destructionMultiplier(chargePercent);
        double destructionMultiplier = cir.getReturnValueD();
        if (multiplier > 1.0F) {
            destructionMultiplier *= multiplier;
        }
        double cappedMultiplier = KiAttackOverhaul.capOverchargeDestructionMultiplier(destructionMultiplier);
        if (Double.compare(cappedMultiplier, cir.getReturnValueD()) != 0) {
            cir.setReturnValue(cappedMultiplier);
        }
    }
}
