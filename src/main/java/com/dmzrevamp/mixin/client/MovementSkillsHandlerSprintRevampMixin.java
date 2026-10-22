package com.dmzrevamp.mixin.client;

import com.dragonminez.client.events.MovementSkillsHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(MovementSkillsHandler.class)
public abstract class MovementSkillsHandlerSprintRevampMixin {
    @ModifyConstant(method = "onClientTick", constant = @org.spongepowered.asm.mixin.injection.Constant(doubleValue = 0.06D), remap = false)
    private static double dmzrevamp$sprintBoostIsFivePercentPerLevel(double original) {
        return 0.05D;
    }
}
