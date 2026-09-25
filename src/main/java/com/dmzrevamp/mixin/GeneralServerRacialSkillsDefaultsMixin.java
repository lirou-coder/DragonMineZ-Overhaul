package com.dmzrevamp.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * DMZ 2.2 split the old flat RacialSkillsConfig into per-race sub-configs.
 * Keep the Overhaul's Namekian default stat list on the new Namekian config.
 * Gson may still overwrite this constructor default with an existing user value.
 */
@Mixin(targets = "com.dragonminez.common.config.GeneralServerConfig$NamekianRacialConfig")
public abstract class GeneralServerRacialSkillsDefaultsMixin {
    private static final String[] DMZREVAMP_ALL_STATS = {"STR", "SKP", "PWR", "DEF", "STM"};

    @Shadow(remap = false)
    private String[] assimilationBoosts;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void dmzrevamp$setNamekianRacialSkillDefaults(CallbackInfo ci) {
        this.assimilationBoosts = DMZREVAMP_ALL_STATS.clone();
    }
}
