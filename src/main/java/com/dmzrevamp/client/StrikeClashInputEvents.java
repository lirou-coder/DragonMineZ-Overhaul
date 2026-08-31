package com.dmzrevamp.client;

import com.dmzrevamp.DmzRevampMod;
import com.dmzrevamp.network.DmzRevampNetwork;
import com.dmzrevamp.network.StrikeClashInputC2SPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Detects the right-click edge independently from DMZ's shared item-use state. */
@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class StrikeClashInputEvents {
    private static boolean wasUseDown;

    private StrikeClashInputEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            ClientStrikeClashState.clear();
            wasUseDown = false;
            return;
        }
        boolean useDown = minecraft.options.keyUse.isDown();
        if (ClientStrikeClashState.isActive() && minecraft.screen == null && useDown && !wasUseDown) {
            DmzRevampNetwork.CHANNEL.sendToServer(new StrikeClashInputC2SPacket());
        }
        wasUseDown = useDown;
    }
}
