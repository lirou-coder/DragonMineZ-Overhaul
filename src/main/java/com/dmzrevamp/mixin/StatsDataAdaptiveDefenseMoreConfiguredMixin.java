package com.dmzrevamp.mixin;

import com.dmzrevamp.config.AdaptiveDefenseMoreConfigured;
import com.dmzrevamp.revamp.combat.AdaptiveDefenseDamageContext;
import com.dragonminez.common.config.CombatConfig;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = StatsData.class, remap = false)
public abstract class StatsDataAdaptiveDefenseMoreConfiguredMixin {
    @Shadow
    @Final
    private Player player;

    @Shadow
    public abstract double getDefense();

    @Shadow
    public abstract double getTotalMultiplier(String stat);

    @Redirect(
            method = "calculatePostMitigationDamage",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dragonminez/common/config/CombatConfig;getCancelDamageEventIfMitigationTooHigh()Z"
            ),
            require = 0
    )
    private boolean dmzrevamp$disableDmzFullNegationWhenConfigured(CombatConfig config) {
        return !AdaptiveDefenseMoreConfigured.get().enable
                && config.getCancelDamageEventIfMitigationTooHigh();
    }

    @Redirect(
            method = "calculatePostMitigationDamage",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dragonminez/common/config/CombatConfig;getEnableAdaptativeDefenseMitigation()Z"
            ),
            require = 0
    )
    private boolean dmzrevamp$disableDmzAdaptiveStepWhenConfigured(CombatConfig config) {
        return !AdaptiveDefenseMoreConfigured.get().enable
                && config.getEnableAdaptativeDefenseMitigation();
    }

    @Inject(method = "calculatePostMitigationDamage", at = @At("RETURN"), cancellable = true, require = 0)
    private void dmzrevamp$applyConfiguredAdaptiveDefense(
            double incomingDamage,
            boolean isGuardBroken,
            double armorPenetration,
            CallbackInfoReturnable<Double> cir
    ) {
        AdaptiveDefenseMoreConfigured.Config config = AdaptiveDefenseMoreConfigured.get();
        double result = cir.getReturnValue();
        if (!config.enable || result <= 0D || incomingDamage <= 0D) return;

        double defense = dmzrevamp$effectiveDefense(isGuardBroken, armorPenetration);
        if (defense <= 0D) return;

        double mitigation = dmzrevamp$curve(dmzrevamp$referenceDamage(incomingDamage) / defense, config);
        AdaptiveDefenseDamageContext.Entry context = AdaptiveDefenseDamageContext.current();
        if (context != null) {
            double efficiency = context.type() == AdaptiveDefenseDamageContext.AttackType.KI
                    ? config.adaptiveDefenseKiAttackEfficiency
                    : config.adaptiveDefenseStrikeAttackEfficiency;
            mitigation = Math.min(config.adaptativeDefenseMitigationCap, mitigation * efficiency);
        }
        cir.setReturnValue(result * (1D - Math.max(0D, mitigation)));
    }

    private double dmzrevamp$effectiveDefense(boolean isGuardBroken, double armorPenetration) {
        double defense = getDefense() * Math.max(1D, getTotalMultiplier("DEF"));
        if (isGuardBroken) {
            defense *= 1D - com.dragonminez.common.config.ConfigManager.getCombatConfig().getDefenseDecayOnGuardBreak();
        }
        if (defense > 0D) defense *= 1D - armorPenetration;
        return defense;
    }

    private static double dmzrevamp$referenceDamage(double incomingDamage) {
        AdaptiveDefenseDamageContext.Entry context = AdaptiveDefenseDamageContext.current();
        return context == null ? incomingDamage : Math.max(incomingDamage, context.totalTechniqueDamage());
    }

    private static double dmzrevamp$curve(
            double damageToDefenseRatio,
            AdaptiveDefenseMoreConfigured.Config config
    ) {
        if (!Double.isFinite(damageToDefenseRatio) || damageToDefenseRatio <= 0D) return 0D;
        double parityRatio = config.adaptativeMitigationParityRatio;
        double parityValue = config.adaptativeMitigationParityValue;
        double zeroRatio = config.adaptativeMitigationZeroRatio;
        double cap = config.adaptativeDefenseMitigationCap;
        double capPoint = 1D / config.adaptiveDefenseCapRatio;

        if (damageToDefenseRatio <= capPoint) return cap;
        if (damageToDefenseRatio < parityRatio) {
            double span = Math.max(0.0001D, parityRatio - capPoint);
            double progress = (parityRatio - damageToDefenseRatio) / span;
            return Math.min(cap, parityValue + (cap - parityValue) * progress);
        }
        if (damageToDefenseRatio < zeroRatio) {
            double progress = (zeroRatio - damageToDefenseRatio)
                    / Math.max(0.0001D, zeroRatio - parityRatio);
            return Math.min(cap, parityValue * progress);
        }
        return 0D;
    }
}
