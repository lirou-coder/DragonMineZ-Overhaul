package com.dmzrevamp.mixin.client;

import com.dmzrevamp.config.KiClashConfigured;
import com.dragonminez.client.clash.ClientBeamClashState;
import com.dragonminez.common.stats.techniques.TechniqueDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets the client keep action input available while a clash is active when
 * transformation-during-clash is enabled by the Overhaul.
 *
 * Target the stable TechniqueDispatcher method itself instead of the compiler
 * generated ClientStatsEvents lambda.  The latter is intentionally unstable
 * between DMZ builds and caused the v2.2 alpha loading crash.
 */
@Mixin(TechniqueDispatcher.class)
public abstract class TechniqueDispatcherClashActionMixin {
    @Inject(
            method = "isActionRestrictedKiAttack",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void dmzrevamp$allowActionsDuringConfiguredClash(CallbackInfoReturnable<Boolean> cir) {
        if (ClientBeamClashState.isActive() && KiClashConfigured.get().allowTransformationMidClash) {
            cir.setReturnValue(false);
        }
    }
}
