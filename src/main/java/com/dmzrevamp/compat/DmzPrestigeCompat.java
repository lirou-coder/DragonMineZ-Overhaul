package com.dmzrevamp.compat;

import com.dmzrevamp.DmzRevampMod;
import com.dmzrevamp.config.LevelingRevampConfig;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DmzPrestigeCompat {
    public static final String DMZ_PRESTIGE_MOD_ID = "dmzprestige";
    public static final String DISABLED_MESSAGE = "DMZ Prestige addon is currently present, so the Prestige function of Overhaul is disabled. Remove the DMZ prestige addon if you want to use the prestige system of Overhaul.";

    private DmzPrestigeCompat() {
    }

    public static boolean isPresent() {
        return ModList.get().isLoaded(DMZ_PRESTIGE_MOD_ID);
    }

    public static void forceDisabled(LevelingRevampConfig.Config config) {
        if (isPresent() && config != null && config.Prestige != null) {
            config.Prestige.enabled = false;
        }
    }

    public static void notifyOnlinePlayersIfDisabled() {
        if (!isPresent()) return;
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        Component message = Component.literal(DISABLED_MESSAGE);
        for (var player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(message);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (isPresent()) {
            event.getEntity().sendSystemMessage(Component.literal(DISABLED_MESSAGE));
        }
    }
}
