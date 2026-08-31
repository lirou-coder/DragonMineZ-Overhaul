package com.dmzrevamp.mixin;

import com.dmzrevamp.config.WeightMovementPenaltyConfig;
import com.dragonminez.server.util.GravityLogic;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GravityLogic.class, remap = false)
public abstract class GravityLogicWeightFlightPenaltyMixin {
    @Inject(method = "getFlyFactor", at = @At("RETURN"), cancellable = true, require = 0)
    private static void dmzrevamp$applyConfiguredWeightPenaltyToFlight(Player player, CallbackInfoReturnable<Double> cir) {
        Double weightPenalty = WeightMovementPenaltyConfig.configuredWeightPenalty(player);
        if (weightPenalty == null || weightPenalty <= 0D) {
            return;
        }

        // DMZ's original fly factor only includes gravity, so weight is applied as a separate multiplier here.
        double flightPenalty = Math.min(0.99D, weightPenalty * WeightMovementPenaltyConfig.flightPenaltyMultiplier());
        cir.setReturnValue(Math.max(0D, cir.getReturnValue() * (1D - flightPenalty)));
    }
}
