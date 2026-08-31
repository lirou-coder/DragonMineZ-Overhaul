package com.dmzrevamp.client;

import com.dmzrevamp.revamp.strike.FlyingStrikeYLock;
import net.minecraft.client.Minecraft;

public final class StrikeYAnchorClientHandler {
    private StrikeYAnchorClientHandler() {
    }

    public static void apply(double anchorY, int remainingTicks) {
        if (Minecraft.getInstance().player != null) {
            FlyingStrikeYLock.syncClientAnchor(Minecraft.getInstance().player, anchorY, remainingTicks);
        }
    }
}
