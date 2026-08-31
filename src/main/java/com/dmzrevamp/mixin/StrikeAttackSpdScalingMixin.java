package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.combat.SpdCombatScalingEvents;
import com.dragonminez.server.events.players.combat.StrikeAttackHandler;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Constant;

@Mixin(StrikeAttackHandler.class)
public abstract class StrikeAttackSpdScalingMixin {
    @ModifyConstant(
            method = "dashForward",
            constant = @Constant(doubleValue = 4.0D),
            remap = false
    )
    private static double dmzrevamp$increaseStrikeDashDistance(double baseDistance, ServerPlayer player, boolean isFlying) {
        return baseDistance * SpdCombatScalingEvents.getStrikeDashDistanceMultiplier(player);
    }

}
