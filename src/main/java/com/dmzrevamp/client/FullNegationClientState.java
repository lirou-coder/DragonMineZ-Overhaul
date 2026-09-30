package com.dmzrevamp.client;

import com.dmzrevamp.DmzRevampMod;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FullNegationClientState {
    private static int suppressionTicks;

    private FullNegationClientState() {
    }

    public static void suppressCurrentHit() {
        suppressionTicks = 2;
        clearHurtAnimation();
    }

    public static boolean suppressesDamageTilt() {
        return suppressionTicks > 0;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || suppressionTicks <= 0) return;
        clearHurtAnimation();
        suppressionTicks--;
    }

    private static void clearHurtAnimation() {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        player.hurtTime = 0;
        player.hurtDuration = 0;
    }
}
