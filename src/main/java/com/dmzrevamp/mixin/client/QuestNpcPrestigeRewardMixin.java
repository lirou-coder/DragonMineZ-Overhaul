package com.dmzrevamp.mixin.client;

import com.dmzrevamp.revamp.prestige.PrestigeSystem;
import com.dragonminez.client.gui.quest.QuestNPCDialogueScreen;
import com.dragonminez.common.quest.Difficulty;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = QuestNPCDialogueScreen.class, remap = false)
public abstract class QuestNpcPrestigeRewardMixin {
    @Redirect(
            method = "renderQuestDetails",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/quest/Difficulty;questRewardMultiplier()D"),
            require = 0
    )
    private double dmzrevamp$showPrestigeQuestReward(Difficulty difficulty) {
        if (Minecraft.getInstance().player == null) return difficulty.questRewardMultiplier();
        StatsData data = StatsProvider.get(StatsCapability.INSTANCE, Minecraft.getInstance().player).resolve().orElse(null);
        return PrestigeSystem.roundedDifficultyValue(difficulty.questRewardMultiplier()
                * (data == null ? 1D : PrestigeSystem.storyRewardMultiplier(data)));
    }
}
