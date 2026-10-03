package com.dmzrevamp.mixin;

import com.dragonminez.common.config.TpSource;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.training.MinigameSessionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MinigameSessionManager.class)
public abstract class MinigameSessionTpRewardMixin {
    /**
     * DMZ 2.2 feeds the player's current single-stat TP cost into the base
     * minigame reward curve. The Overhaul treats that curve's coefficient as
     * the base reward and applies the TRAINING TP-source multiplier afterward,
     * so use the neutral value (1^exponent) here. computeReward is shared by
     * progress packets, final display and the actual awarded TP.
     */
    @Redirect(
            method = "computeReward",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dragonminez/common/stats/StatsData;getSingleStatCost(I)I"
            ),
            remap = false
    )
    private static int dmzrevamp$removeTpcFromMinigameBaseReward(StatsData data, int totalStats) {
        return 1;
    }

    @Redirect(
            method = "computeReward",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dragonminez/common/stats/StatsData;getTpSourceMultiplier(Lcom/dragonminez/common/config/TpSource;)D"
            ),
            remap = false
    )
    private static double dmzrevamp$removeTpcProgressionFromTrainingMultiplier(StatsData data, TpSource source) {
        double multiplier = data.getTpSourceMultiplier(source);
        double tpcProgression = Math.max(0D, data.getProgressionTpGainMultiplier() - 1D);
        return Math.max(0D, multiplier - tpcProgression);
    }
}
