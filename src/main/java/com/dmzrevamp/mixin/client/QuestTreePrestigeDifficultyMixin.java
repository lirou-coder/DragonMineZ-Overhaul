package com.dmzrevamp.mixin.client;

import com.dmzrevamp.revamp.prestige.PrestigeSystem;
import com.dragonminez.client.gui.character.QuestTreeScreen;
import com.dragonminez.common.quest.Difficulty;
import com.dragonminez.common.stats.StatsData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = QuestTreeScreen.class, remap = false)
public abstract class QuestTreePrestigeDifficultyMixin {
    @Shadow
    private StatsData statsData;

    @Redirect(
            method = {"renderHardModeTooltip", "difficultyDescriptionLines"},
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/quest/Difficulty;hpMultiplier()D"),
            require = 0
    )
    private double dmzrevamp$showPrestigeHealthDifficulty(Difficulty difficulty) {
        return PrestigeSystem.roundedDifficultyValue(
                difficulty.hpMultiplier() * (statsData == null ? 1D : PrestigeSystem.storyDifficultyMultiplier(statsData)));
    }

    @Redirect(
            method = {"renderHardModeTooltip", "difficultyDescriptionLines"},
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/quest/Difficulty;damageMultiplier()D"),
            require = 0
    )
    private double dmzrevamp$showPrestigeDamageDifficulty(Difficulty difficulty) {
        return PrestigeSystem.roundedDifficultyValue(
                difficulty.damageMultiplier() * (statsData == null ? 1D : PrestigeSystem.storyDifficultyMultiplier(statsData)));
    }

    @Redirect(
            method = {"renderHardModeTooltip", "difficultyDescriptionLines", "rewardDescription"},
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/quest/Difficulty;questRewardMultiplier()D"),
            require = 0
    )
    private double dmzrevamp$showPrestigeRewardDifficulty(Difficulty difficulty) {
        return PrestigeSystem.roundedDifficultyValue(
                difficulty.questRewardMultiplier() * (statsData == null ? 1D : PrestigeSystem.storyRewardMultiplier(statsData)));
    }
}
