package com.dmzrevamp.mixin;

import com.dmzrevamp.config.LevelingRevampConfig;
import com.dmzrevamp.revamp.prestige.PrestigeSystem;
import com.dragonminez.common.stats.StatsData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StatsData.class)
public abstract class StatsDataPrestigeTpMixin {
    @Inject(method = "getTpTotalMultiplier", at = @At("RETURN"), cancellable = true, remap = false)
    private void dmzrevamp$applyPrestigeTpMultiplier(CallbackInfoReturnable<Double> cir) {
        if (!LevelingRevampConfig.prestigeEnabled()) return;
        // DMZ combines TP sources additively. Prestige is another named source,
        // so only its amount above x1 is added to the existing total.
        cir.setReturnValue(cir.getReturnValueD()
                + PrestigeSystem.tpMultiplier((StatsData) (Object) this) - 1D);
    }

    @Inject(method = "getTpSourceMultiplier", at = @At("RETURN"), cancellable = true, remap = false)
    private void dmzrevamp$applyPrestigeToEveryTpSource(CallbackInfoReturnable<Double> cir) {
        if (!LevelingRevampConfig.prestigeEnabled()) return;
        cir.setReturnValue(cir.getReturnValueD()
                + PrestigeSystem.tpMultiplier((StatsData) (Object) this) - 1D);
    }
}
