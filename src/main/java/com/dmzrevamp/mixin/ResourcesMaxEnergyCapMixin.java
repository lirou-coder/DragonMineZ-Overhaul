package com.dmzrevamp.mixin;

import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Resources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Resources.class)
public abstract class ResourcesMaxEnergyCapMixin {
    @Shadow(remap = false)
    private float currentEnergy;

    @Shadow(remap = false)
    private transient StatsData statsData;

    @Shadow(remap = false)
    public abstract void setPowerRelease(int release);

    @Inject(method = "setCurrentEnergy", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzrevamp$storeDecimalEnergy(float requestedEnergy, CallbackInfo ci) {
        if (requestedEnergy <= 1.0F) {
            setPowerRelease(0);
        }

        float maxEnergy = statsData != null ? statsData.getMaxEnergy() : Float.MAX_VALUE;
        if (!Float.isFinite(maxEnergy) || maxEnergy < 0.0F) {
            maxEnergy = 0.0F;
        }
        currentEnergy = Math.min(Math.max(0.0F, requestedEnergy), maxEnergy);
        ci.cancel();
    }
}
