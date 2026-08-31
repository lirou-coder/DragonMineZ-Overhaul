package com.dmzrevamp.mixin;

import com.dragonminez.common.stats.techniques.TechniqueDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(TechniqueDispatcher.class)
public abstract class TechniqueDispatcherLifetimeCapMixin {
    @ModifyArg(
            method = "executeKiAttack",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/stats/techniques/TechniqueDispatcher;resolvePlayerMaxLifeTicks(Lcom/dragonminez/common/stats/techniques/KiAttackData;F)I"),
            index = 1,
            require = 0,
            remap = false
    )
    private static float dmzrevamp$keepOverchargeFromExtendingLifetime(float chargeMultiplier) {
        return Math.min(chargeMultiplier, 2.0F);
    }
}
