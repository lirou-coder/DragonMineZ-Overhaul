package com.dmzrevamp.mixin;

import com.dragonminez.common.config.CombatConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CombatConfig.class)
public abstract class CombatConfigAdaptiveDefenseDefaultsMixin {
    @Shadow(remap = false)
    private Boolean accurateMobBattlePower;
    @Shadow(remap = false)
    private Double staminaConsumptionRatio;
    @Shadow(remap = false)
    private Double blockStaminaCost;
    @Shadow(remap = false)
    private Double combatFlyBaseSpeed;
    @Shadow(remap = false)
    private Double combatFlySprintSpeed;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void dmzrevamp$setAdaptiveDefenseDefaults(CallbackInfo ci) {
        this.staminaConsumptionRatio = 0.05D;
        this.blockStaminaCost = 0.125D;
        this.combatFlyBaseSpeed = 0.1D;
        this.combatFlySprintSpeed = 0.2D;
        this.accurateMobBattlePower = true;
    }
}
