package com.dmzrevamp.mixin;

import com.dmzrevamp.config.WeightMovementPenaltyConfig;
import com.dragonminez.server.util.GravityLogic;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = GravityLogic.class, remap = false)
public abstract class GravityLogicWeightSpeedPenaltyMixin {
    @Redirect(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/server/util/GravityLogic;getWeightPenaltyFactor(Lnet/minecraft/world/entity/player/Player;)D"),
            require = 0
    )
    private static double dmzrevamp$useConfiguredWeightPenalty(Player player) {
        // When the revamp system is enabled, DMZ's own tick must only apply gravity speed penalties.
        if (WeightMovementPenaltyConfig.isEnabled()) {
            return 0D;
        }
        return WeightMovementPenaltyConfig.weightPenaltyOrOriginal(player);
    }
}
