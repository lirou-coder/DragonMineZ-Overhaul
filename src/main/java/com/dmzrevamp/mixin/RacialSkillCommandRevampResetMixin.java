package com.dmzrevamp.mixin;

import com.dmzrevamp.racial.CustomRacialCooldownEvents;
import com.dmzrevamp.racial.impl.MajinRevampRacialSkill;
import com.dmzrevamp.racial.impl.NamekianRevampRacialSkill;
import com.dmzrevamp.racial.impl.SaiyanRpgZenkaiEvents;
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
                SaiyanRpgZenkaiEvents.resetZenkai(player, data);
                MajinRevampRacialSkill.resetAbsorption(player, data);
                NamekianRevampRacialSkill.resetAssimilation(player, data);
                CustomRacialCooldownEvents.clearAllRacialCooldowns(player);
            });
        }
    }
}
