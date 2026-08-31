package com.dmzrevamp.mixin;

import com.dragonminez.common.network.C2S.TrainingRewardC2S;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Resources;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = TrainingRewardC2S.class, remap = false)
public abstract class TrainingRewardTpBoostMixin {
    @Redirect(
            method = "lambda$handle$0",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/stats/character/Resources;addTrainingPoints(F)V"),
            require = 0
    )
    private void dmzrevamp$scaleMinigameRewardWithTpBonuses(Resources resources, float amount, int levelsCleared, ServerPlayer player, StatsData data) {
        float scaledAmount = amount;
        if (data != null && amount > 0F && Float.isFinite(amount)) {
            // DMZ minigames add TP directly, so apply the player's TP bonus stack here before storing it.
            scaledAmount = (float) Math.max(0D, amount * data.getTpTotalMultiplier());
        }
        resources.addTrainingPoints(scaledAmount);
    }
}
