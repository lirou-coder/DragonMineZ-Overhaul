package com.dmzrevamp.mixin;

import com.dragonminez.common.config.CombatConfig;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.StatsData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StatsData.class)
public abstract class StatsDataAdaptiveDefenseCurveMixin {
    @Inject(method = "computeAdaptativeDefenseMitigation", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void dmzrevamp$useCappedAdaptiveDefenseCurve(double damageToFlatMitigationRatio, CallbackInfoReturnable<Double> cir) {
        if (!Double.isFinite(damageToFlatMitigationRatio) || damageToFlatMitigationRatio <= 0D) {
            cir.setReturnValue(0D);
            return;
        }

        CombatConfig config = ConfigManager.getCombatConfig();
        double parityRatio = Math.max(0.0001D, config.getAdaptativeMitigationParityRatio());
        double parityValue = clamp01(config.getAdaptativeMitigationParityValue());
        double zeroRatio = Math.max(parityRatio + 0.0001D, config.getAdaptativeMitigationZeroRatio());
        double cap = clamp01(config.getAdaptativeDefenseMitigationCap());
        double capRatio = Math.max(0.0001D, parityRatio / zeroRatio);

        double mitigation;
        if (damageToFlatMitigationRatio <= capRatio) {
            mitigation = cap;
        } else if (damageToFlatMitigationRatio < parityRatio) {
            double progress = (parityRatio - damageToFlatMitigationRatio) / (parityRatio - capRatio);
            mitigation = parityValue + ((cap - parityValue) * progress);
        } else {
            double progress = (zeroRatio - damageToFlatMitigationRatio) / (zeroRatio - parityRatio);
            mitigation = parityValue * progress;
        }

        cir.setReturnValue(Math.min(cap, clamp01(mitigation)));
    }

    private static double clamp01(double value) {
        if (!Double.isFinite(value)) {
            return 0D;
        }
        return Math.max(0D, Math.min(1D, value));
    }
}
