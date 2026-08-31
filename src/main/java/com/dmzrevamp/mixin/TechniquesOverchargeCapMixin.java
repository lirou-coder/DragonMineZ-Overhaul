package com.dmzrevamp.mixin;

import com.dmzrevamp.compat.DmzSkillProgressionCompat;
import com.dmzrevamp.revamp.ki.KiAttackOverhaul;
import com.dragonminez.common.stats.techniques.Techniques;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Techniques.class)
public abstract class TechniquesOverchargeCapMixin {
    @Shadow
    private float techniqueChargePercent;

    @Inject(method = "setTechniqueChargePercent", at = @At("HEAD"), cancellable = true, remap = false)
    // Replaces the setter body so no remaining vanilla clamp can hold charge at 200 percent.
    private void dmzrevamp$setExtendedChargePercent(float chargePercent, CallbackInfo ci) {
        chargePercent = DmzSkillProgressionCompat.adjustChargePercent((Techniques) (Object) this, chargePercent);
        this.techniqueChargePercent = Math.max(0.0F, Math.min(KiAttackOverhaul.maxChargePercent(), chargePercent));
        ci.cancel();
    }

    @ModifyConstant(method = "setTechniqueChargePercent", constant = @Constant(floatValue = 200.0F), remap = false)
    // Replaces DMZ's stored charge cap with this mod's 400 percent overcharge cap.
    private float dmzrevamp$extendChargeSetterCap(float original) {
        return KiAttackOverhaul.maxChargePercent();
    }

    @ModifyConstant(method = "load", constant = @Constant(floatValue = 200.0F), remap = false)
    // Lets synced and saved technique charge values keep the extended cap.
    private float dmzrevamp$extendChargeLoadCap(float original) {
        return KiAttackOverhaul.maxChargePercent();
    }
}
