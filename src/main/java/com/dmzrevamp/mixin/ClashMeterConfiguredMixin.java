package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.ki.ConfiguredClashMeter;
import com.dragonminez.common.combat.clash.ClashMeter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Makes native Ki Clashes use the Overhaul tuning while retaining DMZ's seeded meter protocol. */
@Mixin(value = ClashMeter.class, remap = false)
public abstract class ClashMeterConfiguredMixin {
    @Inject(method = "sample", at = @At("HEAD"), cancellable = true)
    private static void dmzrevamp$configuredKiMeter(long seed, float time,
                                                     CallbackInfoReturnable<ClashMeter.Sample> cir) {
        cir.setReturnValue(ConfiguredClashMeter.sampleKi(seed, time));
    }
}
