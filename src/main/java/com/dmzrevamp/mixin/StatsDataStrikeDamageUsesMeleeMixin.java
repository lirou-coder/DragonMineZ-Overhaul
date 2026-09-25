package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.strike.StrikeAttackComputationContext;
import com.dragonminez.common.stats.StatsData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * During creation of an ActiveStrike, use the Overhaul's physical Melee Damage
 * as the Strike Attack base instead of DMZ 2.2's SKP-based Strike Damage.
 */
@Mixin(StatsData.class)
public abstract class StatsDataStrikeDamageUsesMeleeMixin {
    @Inject(method = "getStrikeDamage", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzrevamp$useMeleeDamageForStrikeAttackDamage(CallbackInfoReturnable<Double> cir) {
        if (!StrikeAttackComputationContext.isActive()) {
            return;
        }
        cir.setReturnValue(((StatsData) (Object) this).getMeleeDamage());
    }
}
