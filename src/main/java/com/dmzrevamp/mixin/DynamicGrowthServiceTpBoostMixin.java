package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.growth.DynamicGrowthAwardContext;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.extras.DynamicGrowthStat;
import com.dragonminez.server.dynamicgrowth.DynamicGrowthService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = DynamicGrowthService.class, remap = false)
public abstract class DynamicGrowthServiceTpBoostMixin {
    @ModifyVariable(method = "award", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private static double dmzrevamp$scaleGrowthXpWithTpBonuses(double xp, ServerPlayer player, StatsData data, DynamicGrowthStat stat, double originalXp, LivingEntity target) {
        if (data == null || xp <= 0D || !Double.isFinite(xp)) {
            return xp;
        }

        Double explicitMultiplier = DynamicGrowthAwardContext.tpMultiplierOverride();
        if (explicitMultiplier != null) {
            return Math.max(0D, xp * explicitMultiplier);
        }

        // Dynamic Growth is a training reward, so it follows the same total TP bonus stack as normal TP gains.
        return Math.max(0D, xp * data.getTpTotalMultiplier());
    }
}
