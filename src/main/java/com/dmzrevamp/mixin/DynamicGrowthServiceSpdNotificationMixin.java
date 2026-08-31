package com.dmzrevamp.mixin;

import com.dmzrevamp.config.DmzRevampConfig;
import com.dragonminez.common.stats.extras.DynamicGrowthStat;
import com.dragonminez.server.dynamicgrowth.DynamicGrowthService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DynamicGrowthService.class)
public abstract class DynamicGrowthServiceSpdNotificationMixin {
    @Inject(method = "notifyStatGain", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dmzrevamp$renameSkpGrowthNotification(ServerPlayer player, DynamicGrowthStat stat, int newValue, CallbackInfo ci) {
        if (!dmzrevamp$shouldNotifyStatGain(newValue)) {
            ci.cancel();
            return;
        }
        if (stat == DynamicGrowthStat.SKP) {
            Component message = Component.translatable(
                    "dynamicgrowth.dragonminez.stat_gain",
                    Component.translatable("gui.dragonminez.character_stats.spd"),
                    newValue
            ).withStyle(ChatFormatting.GREEN);
            player.displayClientMessage(message, true);
            player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.35F, 1.25F);
            ci.cancel();
        }
    }

    private static boolean dmzrevamp$shouldNotifyStatGain(int newValue) {
        if (!DmzRevampConfig.ENABLE_DYNAMIC_GROWTH_NOTIFICATION_SUPPRESSION.get()) {
            return true;
        }

        long suppressionStart = Math.max(1L, DmzRevampConfig.DYNAMIC_GROWTH_NOTIFICATION_SUPPRESSION_START.get());
        if (newValue < suppressionStart) {
            return true;
        }

        long interval = 10L;
        long threshold = suppressionStart * 10L;
        while (newValue >= threshold && interval < 1_000_000_000L) {
            interval *= 10;
            threshold *= 10;
        }
        // High-level players only see milestone messages instead of one message for every stat point.
        return newValue % interval == 0;
    }
}
