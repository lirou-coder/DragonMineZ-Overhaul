package com.dmzrevamp.mixin;

import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.extras.DynamicGrowthStat;
import com.dragonminez.server.dynamicgrowth.DynamicGrowthService;
import com.dragonminez.server.events.players.combat.StrikeAttackHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(StrikeAttackHandler.class)
public abstract class StrikeAttackDynamicGrowthMixin {
    @Redirect(
            method = "lambda$applyStrikeDamage$10",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/server/dynamicgrowth/DynamicGrowthService;awardStrike(Lnet/minecraft/server/level/ServerPlayer;Lcom/dragonminez/common/stats/StatsData;Lnet/minecraft/world/entity/LivingEntity;D)V"),
            require = 0,
            remap = false
    )
    private static void dmzrevamp$awardStrikeGrowthAsStrength(ServerPlayer player, StatsData data, LivingEntity target, double damage) {
        double xp = DynamicGrowthService.practiceDamageXp(player, target, (float) damage);
        DynamicGrowthService.award(player, data, DynamicGrowthStat.STR, xp, target);
    }
}
