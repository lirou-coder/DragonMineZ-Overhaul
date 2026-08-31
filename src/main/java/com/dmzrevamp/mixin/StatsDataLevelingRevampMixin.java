package com.dmzrevamp.mixin;

import com.dmzrevamp.config.LevelingRevampConfig;
import com.dmzrevamp.revamp.DmzRevampHelper;
import com.dmzrevamp.revamp.prestige.PrestigeSystem;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.StatsData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StatsData.class)
public abstract class StatsDataLevelingRevampMixin {
    @Redirect(
            method = "calculatePostMitigationDamage",
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(DD)D", ordinal = 1),
            remap = false,
            require = 0
    )
    private double dmzrevamp$applyCustomDefenseCurve(
            double originalReduction,
            double baseCap,
            double incomingDamage,
            boolean isGuardBroken,
            double armorPenetration
    ) {
        if (!com.dmzrevamp.config.DmzRevampConfig.CUSTOM_DEFENSE_AND_SPEED_EFFECTS_CURVE.get()) {
            return Math.min(originalReduction, baseCap);
        }
        StatsData data = (StatsData) (Object) this;
        double defense = data.getDefense() * Math.max(1D, data.getTotalMultiplier("DEF"));
        if (isGuardBroken) {
            defense *= 1D - ConfigManager.getCombatConfig().getDefenseDecayOnGuardBreak();
        }
        if (defense > 0D) defense *= 1D - armorPenetration;
        double expectedMaxDefense = DmzRevampHelper.getDefenseCurveReference(data)
                * data.getStatScaling("DEF");
        return DmzRevampHelper.getConfiguredDefenseStyleEffect(defense, expectedMaxDefense, baseCap);
    }

    @Redirect(method = "calculatePostMitigationDamage", at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/stats/StatsData;getConfiguredMaxValue()I"), remap = false)
    private int dmzrevamp$useAttributeMaximumForDefenseCurve(StatsData data) {
        return LevelingRevampConfig.levelsEnabled() ? PrestigeSystem.attributeFormulaMaximum() : data.getConfiguredMaxValue();
    }

    @Redirect(method = "calculatePostMitigationDamage", at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/stats/StatsData;isMaxLevelValueInsteadOfStats()Z"), remap = false)
    private boolean dmzrevamp$avoidLevelTotalConversionForExplicitAttributeMaximum(StatsData data) {
        return !LevelingRevampConfig.levelsEnabled() && data.isMaxLevelValueInsteadOfStats();
    }

    @Inject(method = "getConfiguredMaxValue", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzrevamp$replaceDmzMaxValue(CallbackInfoReturnable<Integer> cir) {
        if (!LevelingRevampConfig.levelsEnabled()) return;
        cir.setReturnValue(PrestigeSystem.levelCap((StatsData) (Object) this));
    }

    @Inject(method = "getConfiguredMaxTotalStats", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzrevamp$replaceMaximumTotalStats(CallbackInfoReturnable<Integer> cir) {
        if (!LevelingRevampConfig.levelsEnabled()) return;
        cir.setReturnValue(PrestigeSystem.maxAssignableTotal((StatsData) (Object) this));
    }

    @Inject(method = "getMaxAllowedIncreaseForStat", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzrevamp$enforceLevelAndAttributeCaps(String stat, int requested, CallbackInfoReturnable<Integer> cir) {
        if (!LevelingRevampConfig.levelsEnabled()) return;
        StatsData data = (StatsData) (Object) this;
        int totalRemaining = Math.max(0, PrestigeSystem.maxAssignableTotal(data) - data.getStats().getTotalStats());
        int attributeMaximum = PrestigeSystem.effectiveMaximumAttribute(data);
        int attributeRemaining = attributeMaximum == Integer.MAX_VALUE
                ? Integer.MAX_VALUE
                : Math.max(0, attributeMaximum - data.getCurrentStatValue(stat));
        cir.setReturnValue(Math.max(0, Math.min(Math.max(0, requested), Math.min(totalRemaining, attributeRemaining))));
    }
}
