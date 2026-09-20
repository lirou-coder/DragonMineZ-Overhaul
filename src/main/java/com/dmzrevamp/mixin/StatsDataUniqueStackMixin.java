package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.battlepower.UniqueFormStackCombination;
import com.dragonminez.common.stats.StatsData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Runs after normal DMZ/addon return hooks and replaces only the Form + Stack contribution. */
@Mixin(value = StatsData.class, priority = 1)
public abstract class StatsDataUniqueStackMixin {
    @Inject(method = "getTotalMultiplier", at = @At("RETURN"), cancellable = true, remap = false)
    private void dmzrevamp$useUniqueFormStackCombination(String stat, CallbackInfoReturnable<Double> cir) {
        StatsData data = (StatsData) (Object) this;
        if (UniqueFormStackCombination.enabledFor(data)) {
            cir.setReturnValue(UniqueFormStackCombination.replaceFormAndStackInTotal(
                    data, stat, cir.getReturnValueD()));
        }
    }
}
