package com.dmzrevamp.mixin;

import com.dmzrevamp.racial.RevampRacialResetHelper;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.server.commands.RacialSkillCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(value = RacialSkillCommand.class, remap = false)
public abstract class RacialSkillCommandRevampResetMixin {
    @Inject(method = "resetRacialSkills", at = @At("TAIL"), require = 0)
    private static void dmzrevamp$resetAccumulatingRacials(CommandSourceStack source,
                                                           Collection<ServerPlayer> targets,
                                                           CallbackInfoReturnable<Integer> cir) {
        for (ServerPlayer player : targets) {
            StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
                RevampRacialResetHelper.resetAll(player, data);
            });
        }
    }
}
