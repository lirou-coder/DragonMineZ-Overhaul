package com.dmzrevamp.client;

import com.dmzrevamp.DmzRevampMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class DmzRevampClientSetup {
    private DmzRevampClientSetup() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Handwear is drawn by the DMZ player renderer so it can use the same armor arm bones as DMZ armor.
    }
}
