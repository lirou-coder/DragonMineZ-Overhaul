package com.dmzrevamp.mixin.client;

import com.dmzrevamp.client.ScouterClientState;
import com.dragonminez.client.events.ClientStatsEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ClientStatsEvents.class, remap = false)
public abstract class ClientStatsEventsScouterMixin {
    @Redirect(
            method = "lambda$onClientTick$3",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/client/gui/hud/ScouterHUD;setRenderingInfo(Z)V"),
            require = 0
    )
    private static void dmzrevamp$cycleScouterModes(boolean rendering) {
        ScouterClientState.cycleMode();
    }
}
