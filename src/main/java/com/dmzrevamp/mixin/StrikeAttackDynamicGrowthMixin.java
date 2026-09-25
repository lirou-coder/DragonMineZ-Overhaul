package com.dmzrevamp.mixin;

import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.extras.DynamicGrowthStat;
import com.dragonminez.server.dynamicgrowth.DynamicGrowthService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The Overhaul treats Strike Attack practice as physical/STR training instead of
 * DMZ's default SKP training. Hook the stable DynamicGrowthService entry point
 * directly instead of a compiler-generated lambda inside StrikeAttackHandler.
 */
@Mixin(DynamicGrowthService.class)
public abstract class StrikeAttackDynamicGrowthMixin {
    @Inject(method = "awardStrike", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dmzrevamp$awardStrikeGrowthAsStrength(
            ServerPlayer player,
            StatsData data,
            LivingEntity target,
            double damage,
            CallbackInfo ci
    ) {
        if (player == null || data == null || target == null) {
            return;
        }

        double xp = DynamicGrowthService.practiceDamageXp(player, target, (float) damage);
        DynamicGrowthService.award(player, data, DynamicGrowthStat.STR, xp, target);
        ci.cancel();
    }
}
