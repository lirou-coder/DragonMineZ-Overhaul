package com.dmzrevamp.mixin.client;

import com.dragonminez.client.events.ClientStatsEvents;
import com.dragonminez.common.stats.character.Status;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientStatsEvents.class)
public abstract class ClientStatsEventsFusionKiBlastMixin {
    @Redirect(
            method = "canActivateTechnique",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/stats/character/Status;isFusionLeader()Z"),
            remap = false
    )
    private static boolean dmzrevamp$allowKiBlastWhileControllingFusion(Status status) {
        // The server still validates Ki Control, resources, and cooldown; this only lets the fused client send the action.
        return true;
    }

    @Redirect(
            method = "lambda$onClientTick$3",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/stats/character/Status;isFused()Z"),
            remap = false
    )
    private static boolean dmzrevamp$allowBasicKiBlastWhileFused(Status status) {
        return false;
    }

    @Redirect(
            method = "lambda$onClientTick$3",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/stats/character/Status;isFusionLeader()Z"),
            remap = false
    )
    private static boolean dmzrevamp$allowBasicKiBlastForFusionLeader(Status status) {
        return false;
    }
}
