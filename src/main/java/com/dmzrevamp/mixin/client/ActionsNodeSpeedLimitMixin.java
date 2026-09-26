package com.dmzrevamp.mixin.client;

import com.dragonminez.client.gui.radial.nodes.FlightSpeedNode;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FlightSpeedNode.class)
public abstract class ActionsNodeSpeedLimitMixin {
    @Inject(method = "label", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzrevamp$useOverhaulSpeedLimitLabel(StatsData stats, CallbackInfoReturnable<Component> cir) {
        cir.setReturnValue(Component.translatable("gui.action.dmzrevamp.speed_limit"));
    }

    @ModifyConstant(method = "buildOptions", constant = @Constant(intValue = 25), remap = false)
    private int dmzrevamp$allowFivePercentSpeedLimit(int original) {
        return 5;
    }
}
