package com.dmzrevamp.mixin.compat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "com.butterjaffa.noeabosses.client.UniverseSenseRenderer", remap = false)
public abstract class NoeaUniverseSenseRendererMixin {
    @Redirect(
            method = "draw",
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(DD)D", ordinal = 0),
            require = 0
    )
    private static double dmzrevamp$keepOverhaulAuraScale(double entityMinimum, double overhaulScale) {
        return overhaulScale;
    }
}
