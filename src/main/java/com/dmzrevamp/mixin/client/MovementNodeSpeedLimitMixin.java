package com.dmzrevamp.mixin.client;

import com.dmzrevamp.client.OverhaulSpeedLimitNode;
import com.dmzrevamp.client.OverhaulSprintNode;
import com.dragonminez.client.gui.radial.RadialNode;
import com.dragonminez.client.gui.radial.nodes.MovementNode;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(MovementNode.class)
public abstract class MovementNodeSpeedLimitMixin {
    @Inject(method = "buildChildren", at = @At("RETURN"), cancellable = true, remap = false)
    private void dmzrevamp$addOverhaulSpeedLimit(StatsData stats, CallbackInfoReturnable<List<RadialNode>> cir) {
        List<RadialNode> nodes = new ArrayList<>(cir.getReturnValue());
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).label(stats).getContents() instanceof TranslatableContents translated
                    && "skill.dragonminez.sprint".equals(translated.getKey())) {
                nodes.set(i, new OverhaulSprintNode());
                break;
            }
        }
        cir.setReturnValue(nodes);
    }
}
