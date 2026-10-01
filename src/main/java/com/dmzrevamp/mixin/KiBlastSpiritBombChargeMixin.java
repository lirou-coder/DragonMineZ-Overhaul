package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.ki.SpiritBombChargeAbsorption;
import com.dragonminez.common.init.entities.ki.KiBlastEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = KiBlastEntity.class, remap = false)
public abstract class KiBlastSpiritBombChargeMixin {
    @Inject(method = "fireHability", at = @At("TAIL"), remap = false)
    private void dmzrevamp$applySpiritBombContributions(int finalMaxLife, CallbackInfo ci) {
        SpiritBombChargeAbsorption.applyStoredBonusOnFire((KiBlastEntity) (Object) this);
    }
}
