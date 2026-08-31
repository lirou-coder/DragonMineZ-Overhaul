package com.dmzrevamp.mixin;

import com.dragonminez.common.config.GeneralUserConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GeneralUserConfig.class)
public abstract class GeneralUserConfigDefaultsMixin {
    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void dmzrevamp$defaultCameraMovementDuringFlightOff(CallbackInfo ci) {
        ((GeneralUserConfig) (Object) this).setCameraMovementDuringFlight(false);
    }
}
