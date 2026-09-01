package com.dmzrevamp.revamp.forms;

import com.dmzrevamp.compat.DmzSkillProgressionCompat;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class RequiredDmzLevelGuard {
    private RequiredDmzLevelGuard() {}

    public static boolean allows(ServerPlayer player, StatsData data, FormConfig.FormData form) {
        if (player.isCreative()) return true;
        if (DmzSkillProgressionCompat.isRankZeroKaiokenTraining(data, form)) return true;
        int required = form instanceof RequiredDmzLevelForm extension
                ? Math.max(1, extension.dmzrevamp$getRequiredDMZLevel()) : 1;
        if (data.getLevel() >= required) return true;
        long last = player.getPersistentData().getLong("dmzrevampRequiredLevelWarning");
        if (player.level().getGameTime() - last >= 20L) {
            player.getPersistentData().putLong("dmzrevampRequiredLevelWarning", player.level().getGameTime());
            Component warning = Component.literal("you need to be at least on level ")
                    .withStyle(ChatFormatting.RED)
                    .append(Component.literal(Integer.toString(required)).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" to use this form!").withStyle(ChatFormatting.RED));
            player.displayClientMessage(warning, true);
        }
        return false;
    }
}
