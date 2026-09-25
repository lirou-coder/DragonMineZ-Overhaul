package com.dmzrevamp.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * DMZ 2.2 stores Majin absorption settings in MajinRacialConfig instead of
 * directly in RacialSkillsConfig. Preserve the Overhaul's default stat list.
 */
@Mixin(targets = "com.dragonminez.common.config.GeneralServerConfig$MajinRacialConfig")
public abstract class GeneralServerMajinRacialSkillsDefaultsMixin {
    private static final String[] DMZREVAMP_ALL_STATS = {"STR", "SKP", "PWR", "DEF", "STM"};

    @Shadow(remap = false)
    private String[] absorptionBoosts;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void dmzrevamp$setMajinRacialSkillDefaults(CallbackInfo ci) {
        this.absorptionBoosts = DMZREVAMP_ALL_STATS.clone();
    }
}
