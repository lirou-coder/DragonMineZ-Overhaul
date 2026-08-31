package com.dmzrevamp.mixin;

import com.dmzrevamp.config.LevelingRevampConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.dragonminez.common.config.GeneralServerConfig$GameplayConfig")
public abstract class GeneralServerLevelingRevampMixin {
    @Inject(method = "getMaxValue", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzrevamp$exposeLevelingRevampMaximum(CallbackInfoReturnable<Integer> cir) {
        if (LevelingRevampConfig.levelsEnabled()) {
            cir.setReturnValue(LevelingRevampConfig.get().levelsAndAttributes.maxLevel);
        }
    }
}
