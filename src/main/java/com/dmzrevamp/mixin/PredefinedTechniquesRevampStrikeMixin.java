package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.strike.StrikeAttackTemplates;
import com.dragonminez.common.stats.techniques.PredefinedTechniques;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PredefinedTechniques.class)
public abstract class PredefinedTechniquesRevampStrikeMixin {
    @Inject(method = "init", at = @At("RETURN"), remap = false)
    private void dmzrevamp$registerRaceExclusiveStrikes(CallbackInfo ci) {
        StrikeAttackTemplates.registerRaceExclusiveDefaults();
    }
}
