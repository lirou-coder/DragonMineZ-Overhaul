package com.dmzrevamp.mixin.client;

import com.dmzrevamp.racial.CustomRacialActionHelper;
import com.dragonminez.client.gui.radial.nodes.NamekRegenNode;
import com.dragonminez.common.stats.StatsData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NamekRegenNode.class)
public abstract class NamekRegenNodeBioAndroidMixin {
    @Inject(method = "visible", at = @At("RETURN"), cancellable = true, remap = false)
    private void dmzrevamp$showForBioAndroidRevamp(StatsData data, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && "bioandroidrevamp".equalsIgnoreCase(
                CustomRacialActionHelper.getConfiguredRacialSkillId(data))) {
            cir.setReturnValue(true);
        }
    }
}
