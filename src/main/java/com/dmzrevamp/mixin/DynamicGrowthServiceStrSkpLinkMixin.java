package com.dmzrevamp.mixin;

import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.extras.DynamicGrowthStat;
import com.dragonminez.server.dynamicgrowth.DynamicGrowthService;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * DMZ links each practiced stat to a second stat.  In particular, STR and
 * SKP share practice XP, which makes movement growth (SKP) also train STR.
 * The Overhaul awards swimming STR explicitly, so removing only this linked
 * award preserves that intended swimming behaviour.
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
        if (stat == DynamicGrowthStat.STR || stat == DynamicGrowthStat.SKP) {
            ci.cancel();
        }
    }
}
