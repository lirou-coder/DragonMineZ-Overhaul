package com.dmzrevamp.mixin.compat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Makes Better Forms recognize the extra highest tiers supplied by Overhaul. */
@Pseudo
@Mixin(targets = "com.lcd.dmzbetterforms.FormTierTable", remap = false)
public abstract class DmzBetterFormsTierCompatMixin {
    @Inject(method = "getMultiplier", at = @At("HEAD"), cancellable = true, require = 0)
    private static void dmzrevamp$extendOverhaulFormTiers(String race, String formType, int level,
                                                          CallbackInfoReturnable<Double> cir) {
        if (formType == null) return;

        // Better Forms caps its existing tier tables at x2. Overhaul adds one tier
        // beyond its Android, Namekian and Frost Demon arrays, so those forms use
        // the same cap and remain eligible for Better Forms' aura/destruction flow.
        if (("androidforms".equals(formType) && level >= 3)
                || ("superforms".equals(formType) && "namekian".equalsIgnoreCase(race) && level >= 4)
                || ("superforms".equals(formType) && "frostdemon".equalsIgnoreCase(race) && level >= 6)) {
            cir.setReturnValue(2.0D);
        }
    }
}
