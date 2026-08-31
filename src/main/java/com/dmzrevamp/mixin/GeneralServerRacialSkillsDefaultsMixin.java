package com.dmzrevamp.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.dragonminez.common.config.GeneralServerConfig$RacialSkillsConfig")
public abstract class GeneralServerRacialSkillsDefaultsMixin {
    private static final String[] DMZREVAMP_ALL_STATS = {"STR", "SKP", "PWR", "DEF", "STM"};

    @Shadow(remap = false)
    private String[] namekianAssimilationBoosts;
    @Shadow(remap = false)
    private String[] majinAbsorptionBoosts;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void dmzrevamp$setRacialSkillDefaults(CallbackInfo ci) {
        this.namekianAssimilationBoosts = DMZREVAMP_ALL_STATS.clone();
        this.majinAbsorptionBoosts = DMZREVAMP_ALL_STATS.clone();
    }
}
