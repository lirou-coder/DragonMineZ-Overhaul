package com.dmzrevamp.client;

import com.dmzrevamp.DmzRevampMod;
import com.dmzrevamp.config.KiClashConfigured;
import com.dmzrevamp.network.ClashTransformChargeC2SPacket;
import com.dmzrevamp.network.DmzRevampNetwork;
import com.dragonminez.client.clash.ClientBeamClashState;
import com.dragonminez.client.util.KeyBinds;
import com.dragonminez.common.network.C2S.UpdateStatC2S;
import com.dragonminez.common.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Restores transformation input while DMZ's clash input lock is active. */
@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, value = Dist.CLIENT)
public final class ClientKiClashTransformationEvents {
    private static boolean wasKiChargeDown;
    private static boolean wasActionDown;

    private ClientKiClashTransformationEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null || !ClientBeamClashState.isActive()) {
            if (wasKiChargeDown && minecraft.player != null) {
                NetworkHandler.sendToServer(new UpdateStatC2S(UpdateStatC2S.StatAction.CHARGE_KI, false));
            }
            if (wasActionDown && minecraft.player != null) {
                NetworkHandler.sendToServer(new UpdateStatC2S(UpdateStatC2S.StatAction.ACTION_CHARGE, false));
                DmzRevampNetwork.CHANNEL.sendToServer(new ClashTransformChargeC2SPacket(false));
            }
            wasKiChargeDown = false;
            wasActionDown = false;
            return;
        }

        boolean enabled = KiClashConfigured.get().allowTransformationMidClash;
        boolean kiChargeDown = enabled
                && (KeyBinds.KI_CHARGE.isDown() || KeyBinds.isPhysicallyDown(KeyBinds.KI_CHARGE));
        boolean actionDown = enabled
                && (KeyBinds.ACTION_KEY.isDown() || KeyBinds.isPhysicallyDown(KeyBinds.ACTION_KEY));
        // DMZ may clear either state while executing its normal combat tick.
        // Reassert both physical inputs after DMZ's handler on every clash tick.
        NetworkHandler.sendToServer(new UpdateStatC2S(UpdateStatC2S.StatAction.CHARGE_KI, kiChargeDown));
        NetworkHandler.sendToServer(new UpdateStatC2S(UpdateStatC2S.StatAction.ACTION_CHARGE, actionDown));
        // DMZ's own client tick can report ACTION_CHARGE=false while its clash lock is active.
        // This heartbeat preserves the physical key state authoritatively on the server.
        DmzRevampNetwork.CHANNEL.sendToServer(new ClashTransformChargeC2SPacket(actionDown));
        wasKiChargeDown = kiChargeDown;
        wasActionDown = actionDown;
    }
}
