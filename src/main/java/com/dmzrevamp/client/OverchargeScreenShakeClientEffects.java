package com.dmzrevamp.client;

import com.dmzrevamp.DmzRevampMod;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class OverchargeScreenShakeClientEffects {
    private static int ticksRemaining;
    private static int maxTicks;
    private static float intensity;

    private OverchargeScreenShakeClientEffects() {
    }

    public static void start(float newIntensity, int ticks) {
        if (ticks <= 0) {
            return;
        }
        intensity = Math.max(intensity, Math.max(0.0F, newIntensity));
        ticksRemaining = Math.max(ticksRemaining, ticks);
        maxTicks = Math.max(maxTicks, ticks);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ticksRemaining <= 0) {
            return;
        }
        ticksRemaining--;
        if (ticksRemaining <= 0) {
            maxTicks = 0;
            intensity = 0.0F;
        }
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (ticksRemaining <= 0 || maxTicks <= 0 || minecraft.player == null) {
            return;
        }
        float fade = ticksRemaining / (float) maxTicks;
        float time = minecraft.player.tickCount + minecraft.getFrameTime();
        float shake = (float) Math.sin(time * 2.3F) * intensity * fade;
        event.setRoll(event.getRoll() + shake);
        event.setPitch(event.getPitch() + (float) Math.sin(time * 1.7F) * intensity * 0.2F * fade);
        event.setYaw(event.getYaw() + (float) Math.cos(time * 1.3F) * intensity * 0.2F * fade);
    }
}
