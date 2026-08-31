package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.combat.ParryStaminaEvents;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.server.events.players.combat.CombatEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CombatEvent.class)
public abstract class CombatEventParryStaminaMixin {
    @Inject(
            method = "lambda$onLivingHurt$4",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/stats/character/Resources;removeStamina(F)V", ordinal = 0),
            require = 0,
            remap = false
    )
    private static void dmzrevamp$rememberParryStamina(double finalHealingReduction, Player victim,
                                                       DamageSource source, double[] currentDamage,
                                                       double finalDefensePenetration, boolean[] wasBlocked,
                                                       boolean[] wasParry, StatsData victimData,
                                                       CallbackInfo ci) {
        if (wasParry[0] && victim instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            ParryStaminaEvents.rememberPreParryStamina(serverPlayer, victimData);
        }
    }
}
