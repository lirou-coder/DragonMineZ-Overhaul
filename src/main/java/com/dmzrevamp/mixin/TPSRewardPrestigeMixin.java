package com.dmzrevamp.mixin;

import com.dmzrevamp.config.LevelingRevampConfig;
import com.dmzrevamp.revamp.prestige.PrestigeSystem;
import com.dragonminez.common.quest.rewards.TPSReward;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(TPSReward.class)
public abstract class TPSRewardPrestigeMixin {
    @ModifyVariable(method = "giveReward(Lnet/minecraft/server/level/ServerPlayer;D)V", at = @At("HEAD"), argsOnly = true, ordinal = 0, remap = false)
    private double dmzrevamp$applyPrestigeStoryReward(double multiplier, ServerPlayer player) {
        if (!LevelingRevampConfig.prestigeEnabled()) return multiplier;
        StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
        return data == null ? multiplier : PrestigeSystem.roundedDifficultyValue(
                multiplier * PrestigeSystem.storyRewardMultiplier(data))
                * PrestigeSystem.tpMultiplier(data);
    }
}
