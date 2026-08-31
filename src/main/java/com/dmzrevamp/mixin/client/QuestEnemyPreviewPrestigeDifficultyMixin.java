package com.dmzrevamp.mixin.client;

import com.dmzrevamp.revamp.prestige.PrestigeSystem;
import com.dragonminez.common.quest.Difficulty;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "com.dragonminez.client.gui.quest.preview.QuestEnemyPreview$Target", remap = false)
public abstract class QuestEnemyPreviewPrestigeDifficultyMixin {
    @Redirect(
            method = "<init>",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/quest/Difficulty;hpMultiplier()D"),
            require = 0
    )
    private double dmzrevamp$showPrestigePreviewHealth(Difficulty difficulty) {
        return PrestigeSystem.roundedDifficultyValue(difficulty.hpMultiplier() * dmzrevamp$prestigeDifficulty());
    }

    @Redirect(
            method = "<init>",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/quest/Difficulty;damageMultiplier()D"),
            require = 0
    )
    private double dmzrevamp$showPrestigePreviewDamage(Difficulty difficulty) {
        return PrestigeSystem.roundedDifficultyValue(difficulty.damageMultiplier() * dmzrevamp$prestigeDifficulty());
    }

    private static double dmzrevamp$prestigeDifficulty() {
        if (Minecraft.getInstance().player == null) return 1D;
        StatsData data = StatsProvider.get(StatsCapability.INSTANCE, Minecraft.getInstance().player).resolve().orElse(null);
        return data == null ? 1D : PrestigeSystem.storyDifficultyMultiplier(data);
    }
}
