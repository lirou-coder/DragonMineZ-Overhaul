package com.dmzrevamp.compat;

import com.dmzrevamp.DmzRevampMod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Ensures players joining a world where Noea realism was already enabled also see the warning. */
@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class NoeaRealisticWarningEvents {
    private NoeaRealisticWarningEvents() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!NoeaCompat.isLoaded() || event.getEntity().level().isClientSide()) return;
        MinecraftServer server = event.getEntity().getServer();
        if (server == null || !enabled(server)) return;
        event.getEntity().sendSystemMessage(Component.literal(
                        "Noea realistic mode detected. Overhaul does not work well with it; you will encounter issues. "
                                + "Disable realistic mode and restore configs for better gameplay.")
                .withStyle(ChatFormatting.RED));
    }

    private static boolean enabled(MinecraftServer server) {
        try {
            Class<?> service = Class.forName("com.butterjaffa.noeabosses.RealisticModeService");
            return (boolean) service.getMethod("isEnabled", MinecraftServer.class).invoke(null, server);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}
