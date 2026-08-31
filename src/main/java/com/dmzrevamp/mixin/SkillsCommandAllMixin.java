package com.dmzrevamp.mixin;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ProgressionSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.server.commands.SkillsCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.List;

@Mixin(value = SkillsCommand.class, remap = false)
public abstract class SkillsCommandAllMixin {
    @Inject(method = "setSkill", at = @At("HEAD"), cancellable = true, require = 0)
    private static void dmzrevamp$setAllSkills(CommandSourceStack source, Collection<ServerPlayer> targets,
                                               String skillName, int level,
                                               CallbackInfoReturnable<Integer> cir) {
        if (!"all".equalsIgnoreCase(skillName)) return;

        var config = ConfigManager.getSkillsConfig();
        List<String> skills = config.getSkills().keySet().stream()
                .filter(skill -> !config.getKiSkills().contains(skill)
                        && !config.getStackSkills().contains(skill)
                        && !config.getFormSkills().contains(skill)
                        && !config.getStrikeSkills().contains(skill))
                .toList();
        for (ServerPlayer player : targets) {
            StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
                for (String skill : skills) data.getSkills().setSkillLevel(skill, level);
                NetworkHandler.sendToTrackingEntityAndSelf(new ProgressionSyncS2C(player), player);
            });
        }

        boolean log = ConfigManager.getServerConfig().getGameplay().getCommandOutputOnConsole();
        int targetCount = targets.size();
        source.sendSuccess(() -> Component.literal("Set all " + skills.size() + " listed skills to level "
                + level + " for " + targetCount + " player" + (targetCount == 1 ? "." : "s.")), log);
        cir.setReturnValue(targetCount);
    }
}
