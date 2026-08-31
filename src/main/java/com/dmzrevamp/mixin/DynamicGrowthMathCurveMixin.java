package com.dmzrevamp.mixin;

import com.dmzrevamp.config.DynamicGrowthCurveConfig;
import com.dragonminez.common.stats.extras.DynamicGrowthMath;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = DynamicGrowthMath.class, remap = false)
public abstract class DynamicGrowthMathCurveMixin {
    @Inject(method = "requiredXp", at = @At("RETURN"), cancellable = true, require = 0)
    private static void dmzrevamp$applyConfiguredDynamicGrowthCurve(int currentStat, CallbackInfoReturnable<Integer> cir) {
        // Lets the JSON file replace DMZ's fixed curve while still preserving DMZ when the file disables it.
        cir.setReturnValue(DynamicGrowthCurveConfig.requiredXpOrOriginal(currentStat, cir.getReturnValue()));
    }
}
