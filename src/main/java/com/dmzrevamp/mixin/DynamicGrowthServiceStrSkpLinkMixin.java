package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.growth.DynamicGrowthAwardContext;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.extras.DynamicGrowthStat;
import com.dragonminez.server.dynamicgrowth.DynamicGrowthService;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * DMZ can award a linked stat alongside the stat explicitly trained. Suppress
 * that second award only while an Overhaul custom activity is being credited;
 * native DMZ activities retain their configured linked-stat behavior.
 */
@Mixin(value = DynamicGrowthService.class, remap = false)
public abstract class DynamicGrowthServiceStrSkpLinkMixin {

    @Inject(
            method = "award",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dragonminez/server/dynamicgrowth/DynamicGrowthService;addPracticeXp(Lnet/minecraft/server/level/ServerPlayer;Lcom/dragonminez/common/stats/StatsData;Lcom/dragonminez/common/stats/extras/DynamicGrowthStat;D)V",
                    ordinal = 1
            ),
            cancellable = true,
            require = 0
    )
    private static void dmzrevamp$disableStrSkpLinkedXp(ServerPlayer player, StatsData data,
                                                         DynamicGrowthStat stat, double baseXp,
                                                         net.minecraft.world.entity.LivingEntity repeatedTarget,
                                                         CallbackInfo ci) {
        if (DynamicGrowthAwardContext.isCustomGrowthAward()) {
            ci.cancel();
        }
    }
}
