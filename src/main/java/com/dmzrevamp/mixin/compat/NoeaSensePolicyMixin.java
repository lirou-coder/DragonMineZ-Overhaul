package com.dmzrevamp.mixin.compat;

import com.dmzrevamp.client.KiSenseDangerStyle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.butterjaffa.noeabosses.sense.SensePolicy", remap = false)
public abstract class NoeaSensePolicyMixin {
    @Inject(method = "radius", at = @At("RETURN"), cancellable = true, require = 0)
    private static void dmzrevamp$useOverhaulAuraScale(double targetPower, double ownPower,
                                                       CallbackInfoReturnable<Double> cir) {
        double ratio = ownPower > 0D && Double.isFinite(targetPower) && Double.isFinite(ownPower)
                ? Math.max(0D, targetPower / ownPower) : 0D;
        cir.setReturnValue(KiSenseDangerStyle.searchAuraScale(ratio) / 2.8D);
    }

    @Inject(method = "auraColor", at = @At("RETURN"), cancellable = true, require = 0)
    private static void dmzrevamp$useOverhaulAuraColor(double radius, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(KiSenseDangerStyle.colorForAuraScale(radius * 2.8D));
    }
}
