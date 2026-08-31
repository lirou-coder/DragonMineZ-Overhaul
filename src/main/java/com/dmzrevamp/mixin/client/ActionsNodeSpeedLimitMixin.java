package com.dmzrevamp.mixin.client;

import com.dmzrevamp.client.radial.SpeedLimitNode;
import com.dragonminez.client.gui.radial.RadialNode;
import com.dragonminez.client.gui.radial.nodes.ActionsNode;
import com.dragonminez.common.stats.StatsData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ActionsNode.class)
public abstract class ActionsNodeSpeedLimitMixin {
    @Inject(method = "buildChildren", at = @At("RETURN"), remap = false)
    private void dmzrevamp$addSpeedLimit(StatsData data, CallbackInfoReturnable<List<RadialNode>> cir) {
        cir.getReturnValue().add(3, new SpeedLimitNode());
    }
}
