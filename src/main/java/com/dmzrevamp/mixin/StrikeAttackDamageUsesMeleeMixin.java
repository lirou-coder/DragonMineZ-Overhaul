package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.strike.StrikeAttackComputationContext;
import com.dragonminez.server.events.players.combat.StrikeAttackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Marks the stable Strike Attack construction methods. The actual StatsData
 * substitution lives in StatsDataStrikeDamageUsesMeleeMixin, avoiding fragile
 * lambda$startStrike$N selectors.
 */
@Mixin(StrikeAttackHandler.class)
public abstract class StrikeAttackDamageUsesMeleeMixin {
    @Inject(
            method = {
                    "startStrike",
                    "startTargetlessStrike",
                    "startSlam",
                    "startDimensionalPunch",
                    "startDimensionalSlash"
            },
            at = @At("HEAD"),
            remap = false
    )
    private static void dmzrevamp$beginStrikeDamageCalculation(CallbackInfo ci) {
        StrikeAttackComputationContext.enter();
    }

    @Inject(
            method = {
                    "startStrike",
                    "startTargetlessStrike",
                    "startSlam",
                    "startDimensionalPunch",
                    "startDimensionalSlash"
            },
            at = @At("RETURN"),
            remap = false
    )
    private static void dmzrevamp$finishStrikeDamageCalculation(CallbackInfo ci) {
        StrikeAttackComputationContext.exit();
    }
}
