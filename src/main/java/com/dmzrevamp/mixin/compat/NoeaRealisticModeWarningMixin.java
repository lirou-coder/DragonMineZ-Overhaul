package com.dmzrevamp.mixin.compat;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.server.ServerStartedEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.butterjaffa.noeabosses.RealisticModeService", remap = false)
public abstract class NoeaRealisticModeWarningMixin {
    @Unique
    private static final Component DMZREVAMP_WARNING = Component.literal(
            "Noea realistic mode detected. Overhaul does not work well with it; you will encounter issues. "
                    + "Disable realistic mode and restore configs for better gameplay.")
            .withStyle(ChatFormatting.RED);

    @Inject(method = "setEnabled", at = @At("RETURN"), require = 0)
    private static void dmzrevamp$warnWhenEnabled(MinecraftServer server, boolean enabled,
                                                  CallbackInfoReturnable<Integer> cir) {
        if (enabled) dmzrevamp$warn(server);
    }

    @Inject(method = "onServerStarted", at = @At("RETURN"), require = 0)
    private static void dmzrevamp$warnWhenAlreadyEnabled(ServerStartedEvent event, CallbackInfo ci) {
        try {
            Class<?> service = Class.forName("com.butterjaffa.noeabosses.RealisticModeService");
            if ((boolean) service.getMethod("isEnabled", MinecraftServer.class).invoke(null, event.getServer())) {
                dmzrevamp$warn(event.getServer());
            }
        } catch (ReflectiveOperationException ignored) {
        }
    }

    @Unique
    private static void dmzrevamp$warn(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) player.sendSystemMessage(DMZREVAMP_WARNING);
    }
}
