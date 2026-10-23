package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.strike.StrikeAttackComputationContext;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.server.events.players.combat.StrikeAttackHandler;
import org.spongepowered.asm.mixin.injection.Redirect;
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
    /**
     * DMZ 2.2 computes strike damage in several handler methods and synthetic
     * lambdas. Those paths do not all run inside the old construction context,
     * so replace the stat at the actual StrikeAttackHandler call sites.
     */
    @Redirect(
            method = {
                    "lambda$startStrike$*",
                    "startTargetlessStrike",
                    "startSlam",
                    "startDimensionalPunch",
                    "startDimensionalSlash",
                    "lambda$clashStrikeDamage$*"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dragonminez/common/stats/StatsData;getStrikeDamage()D"
            ),
            remap = false
    )
    private static double dmzrevamp$useMeleeDamageForStrikeAttacks(StatsData stats) {
        return stats.getMeleeDamage();
    }

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
