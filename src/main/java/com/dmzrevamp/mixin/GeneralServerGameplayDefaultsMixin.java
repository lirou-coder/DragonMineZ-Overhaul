package com.dmzrevamp.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.dragonminez.common.config.GeneralServerConfig$GameplayConfig")
public abstract class GeneralServerGameplayDefaultsMixin {
    private static final String[] DMZREVAMP_ALL_STATS = {"STR", "SKP", "PWR", "DEF", "STM"};

    @Shadow(remap = false)
    private Integer maxValue;
    @Shadow(remap = false)
    private String[] fusionBoosts;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void dmzrevamp$setGameplayDefaults(CallbackInfo ci) {
        this.maxValue = 1000000;
        this.fusionBoosts = DMZREVAMP_ALL_STATS.clone();
    }
}
