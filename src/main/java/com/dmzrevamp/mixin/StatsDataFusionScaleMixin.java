package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.fusion.FusionRevampLogic;
import com.dragonminez.common.stats.StatsData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StatsData.class)
public abstract class StatsDataFusionScaleMixin {
    @Inject(method = "getStatScaling", at = @At("RETURN"), cancellable = true, remap = false)
    private void dmzrevamp$addFusionPartnerStatScale(String statName, CallbackInfoReturnable<Double> cir) {
        cir.setReturnValue(FusionRevampLogic.addPartnerScale((StatsData) (Object) this, statName, cir.getReturnValueD()));
    }
}
