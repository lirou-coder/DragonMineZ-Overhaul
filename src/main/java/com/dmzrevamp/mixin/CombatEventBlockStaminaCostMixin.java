package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.combat.StaminaCostScaling;
import com.dragonminez.common.config.CombatConfig;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.server.events.players.combat.CombatEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CombatEvent.class)
public abstract class CombatEventBlockStaminaCostMixin {
    @Redirect(
            method = "lambda$onLivingHurt$4",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/config/CombatConfig;getBlockStaminaCost()Ljava/lang/Double;"),
            require = 0,
            remap = false
    )
    private static Double dmzrevamp$reduceBlockStaminaCostByTransformedStrength(CombatConfig config,
                                                                                double finalHealingReduction, Player victim,
                                                                                DamageSource source, double[] currentDamage,
                                                                                double finalDefensePenetration, boolean[] wasBlocked,
                                                                                boolean[] wasParry, StatsData victimData) {
        if (wasParry[0]) {
            return 0D;
        }
        return (double) StaminaCostScaling.applyTransformedStrengthDivisor(victimData, config.getBlockStaminaCost().floatValue());
    }
}
