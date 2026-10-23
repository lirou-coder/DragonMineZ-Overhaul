package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.fusion.FusionRevampLogic;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.server.util.FusionDanceAligner;
import com.dragonminez.server.util.FusionLogic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keeps the fusion dance's early warning consistent with configured fusion eligibility. */
@Mixin(value = FusionDanceAligner.class, remap = false)
public abstract class FusionDanceAlignerBlockerMixin {
    @Redirect(
            method = "pair",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dragonminez/server/util/FusionLogic;metamoruBlocker(Lcom/dragonminez/common/stats/StatsData;Lcom/dragonminez/common/stats/StatsData;)Ljava/lang/String;"
            ),
            remap = false
    )
    private static String dmzrevamp$useConfiguredMetamoruEligibility(StatsData leaderData, StatsData partnerData) {
        return FusionRevampLogic.configuredMetamoruBlocker(leaderData, partnerData);
    }
}
