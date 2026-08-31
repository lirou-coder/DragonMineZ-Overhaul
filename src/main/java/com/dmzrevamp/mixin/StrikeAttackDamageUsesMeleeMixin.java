package com.dmzrevamp.mixin;

import com.dragonminez.common.stats.StatsData;
import com.dragonminez.server.events.players.combat.StrikeAttackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(StrikeAttackHandler.class)
public abstract class StrikeAttackDamageUsesMeleeMixin {
    @Redirect(
            method = "lambda$startStrike$4",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/stats/StatsData;getStrikeDamage()D"),
            remap = false
    )
    private static double dmzrevamp$useMeleeDamageForStrikeAttackDamage(StatsData data) {
        return data.getMeleeDamage();
    }
}
