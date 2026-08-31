package com.dmzrevamp.mixin;

import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.techniques.StrikeAttackData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(StrikeAttackData.class)
public abstract class StrikeAttackKiCostUsesMeleeMixin {
    @Redirect(
            method = "getCalculatedCost",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/stats/StatsData;getStrikeDamageNoForms()D"),
            remap = false
    )
    private double dmzrevamp$useMeleeDamageForStrikeAttackKiCost(StatsData data) {
        return data.getMeleeDamageNoMultipliers();
    }
}
