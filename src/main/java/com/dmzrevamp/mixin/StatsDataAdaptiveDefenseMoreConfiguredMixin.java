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

    @Shadow
    public abstract double getFormMultiplier(String stat);

    @Shadow
    public abstract double getStackFormMultiplier(String stat);

    @Shadow public abstract com.dragonminez.common.stats.character.Character getCharacter();
    @Shadow public abstract com.dragonminez.common.stats.character.Resources getResources();
    // DMZ 2.2 exposes resource maxima as floats.  Shadow descriptors must
    // match exactly or Mixin aborts class transformation during startup.
    @Shadow public abstract float getMaxEnergy();
    @Shadow public abstract float getMaxStamina();

    @Redirect(
            method = "calculatePostMitigationDamage",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dragonminez/common/config/CombatConfig;getCancelDamageEventIfMitigationTooHigh()Z"
            ),
            require = 0
    )
    private boolean dmzrevamp$disableDmzFullNegationWhenConfigured(CombatConfig config) {
        return !AdaptiveDefenseMoreConfigured.get().adaptiveDefense.enabled
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
        return !AdaptiveDefenseMoreConfigured.get().adaptiveDefense.enabled
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
        AdaptiveDefenseMoreConfigured.AdaptiveDefense adaptive = config.adaptiveDefense;
        double result = cir.getReturnValue();
        if (result <= 0D || incomingDamage <= 0D) return;

        if (adaptive.enabled) {
            double defense = dmzrevamp$effectiveDefense(isGuardBroken, armorPenetration);
            if (defense > 0D) {
                double mitigation = dmzrevamp$curve(dmzrevamp$referenceDamage(incomingDamage) / defense, adaptive);
                AdaptiveDefenseDamageContext.Entry context = AdaptiveDefenseDamageContext.current();
                if (context != null) {
                    double efficiency = context.type() == AdaptiveDefenseDamageContext.AttackType.KI
                            ? adaptive.adaptiveDefenseKiAttackEfficiency
                            : adaptive.adaptiveDefenseStrikeAttackEfficiency;
                    mitigation = Math.min(adaptive.adaptativeDefenseMitigationCap, mitigation * efficiency);
                }
                result *= 1D - Math.max(0D, mitigation);
            }
        }
        AdaptiveDefenseMoreConfigured.FormReduction divisor = config.formReduction;
        double formDefense = Math.max(1.0E-9D, getFormMultiplier("DEF"));
        double stackDefense = Math.max(1.0E-9D, getStackFormMultiplier("DEF"));
        if (divisor.enabled && (Math.abs(formDefense - 1D) > 1.0E-9D
                || Math.abs(stackDefense - 1D) > 1.0E-9D)) {
            double multi = formDefense * stackDefense;
            double influence = dmzrevamp$divisorInfluence(divisor);
            double damageDivisor = 1D + (multi - 1D) * influence * divisor.formDivisorMulti;
            if (divisor.formReductionCap < 1D) {
                damageDivisor = Math.min(damageDivisor, 1D / (1D - divisor.formReductionCap));
            }
            if (Double.isFinite(damageDivisor) && damageDivisor > 0D) result /= damageDivisor;
        }
        cir.setReturnValue(result);
    }

    private double dmzrevamp$divisorInfluence(AdaptiveDefenseMoreConfigured.FormReduction config) {
        double sum = 0D;
        int count = 0;
        if (config.masteryInfluence) {
            // Read active form masteries directly from the character data. A base
            // form has no active multiplier and therefore never reaches this path.
            var character = getCharacter();
            double first = character.hasActiveForm() ? character.getFormMasteries().getMastery(character.getActiveFormGroup(), character.getActiveForm()) : 100D;
            double second = character.hasActiveStackForm() ? character.getStackFormMasteries().getMastery(character.getActiveStackFormGroup(), character.getActiveStackForm()) : first;
            double mastery = character.hasActiveForm() && character.hasActiveStackForm() ? (first + second) / 2D : (character.hasActiveForm() ? first : second);
            double normalized = Math.max(0D, Math.min(100D, mastery)) / 100D;
            sum += config.zeroMasteryMulti + (1D - config.zeroMasteryMulti) * normalized;
            count++;
        }
        if (config.currentHealthInfluence) { sum += dmzrevamp$resourceRatio(player.getHealth(), player.getMaxHealth(), config.zeroHealthMulti); count++; }
        if (config.currentKiInfluence) { sum += dmzrevamp$resourceRatio(getResources().getCurrentEnergy(), getMaxEnergy(), config.zeroKiMulti); count++; }
        if (config.currentStaminaInfluence) { sum += dmzrevamp$resourceRatio(getResources().getCurrentStamina(), getMaxStamina(), config.zeroStaminaInfluence); count++; }
        return count == 0 ? 1D : sum / count;
    }

    private static double dmzrevamp$resourceRatio(double current, double maximum, double zero) {
        double ratio = maximum > 0D ? Math.max(0D, Math.min(1D, current / maximum)) : 1D;
        return zero + (1D - zero) * ratio;
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
            AdaptiveDefenseMoreConfigured.AdaptiveDefense config
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
