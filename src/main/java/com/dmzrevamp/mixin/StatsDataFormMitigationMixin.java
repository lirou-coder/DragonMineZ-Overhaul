package com.dmzrevamp.mixin;

import com.dmzrevamp.config.FormMitigationConfig;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = StatsData.class, remap = false)
public abstract class StatsDataFormMitigationMixin {
    @Shadow @Final private Player player;
    @Shadow public abstract double getFormMultiplier(String stat);
    @Shadow public abstract double getStackFormMultiplier(String stat);
    @Shadow public abstract com.dragonminez.common.stats.character.Character getCharacter();
    @Shadow public abstract com.dragonminez.common.stats.character.Resources getResources();
    @Shadow public abstract float getMaxEnergy();
    @Shadow public abstract float getMaxStamina();

    @Inject(method = "calculatePostMitigationDamage", at = @At("RETURN"), cancellable = true, require = 0)
    private void dmzrevamp$applyFormMitigation(
            double incomingDamage,
            boolean isGuardBroken,
            double armorPenetration,
            CallbackInfoReturnable<Double> cir
    ) {
        FormMitigationConfig.FormReduction config = FormMitigationConfig.get().formReduction;
        if (!config.enabled) return;

        double formDefense = Math.max(1.0E-9D, getFormMultiplier("DEF"));
        double stackDefense = Math.max(1.0E-9D, getStackFormMultiplier("DEF"));
        if (Math.abs(formDefense - 1D) <= 1.0E-9D && Math.abs(stackDefense - 1D) <= 1.0E-9D) return;

        double result = cir.getReturnValue();
        if (result <= 0D || incomingDamage <= 0D) return;

        double formAndStackDefense = formDefense * stackDefense;
        double influence = dmzrevamp$divisorInfluence(config);
        double divisor = 1D + (formAndStackDefense - 1D) * influence * config.formDivisorMulti;
        if (config.formReductionCap < 1D) {
            divisor = Math.min(divisor, 1D / (1D - config.formReductionCap));
        }
        if (Double.isFinite(divisor) && divisor > 0D) cir.setReturnValue(result / divisor);
    }

    private double dmzrevamp$divisorInfluence(FormMitigationConfig.FormReduction config) {
        double sum = 0D;
        int count = 0;
        if (config.masteryInfluence) {
            var character = getCharacter();
            double formMastery = character.hasActiveForm()
                    ? character.getFormMasteries().getMastery(character.getActiveFormGroup(), character.getActiveForm())
                    : 100D;
            double stackMastery = character.hasActiveStackForm()
                    ? character.getStackFormMasteries().getMastery(character.getActiveStackFormGroup(), character.getActiveStackForm())
                    : formMastery;
            double mastery = character.hasActiveForm() && character.hasActiveStackForm()
                    ? (formMastery + stackMastery) / 2D
                    : (character.hasActiveForm() ? formMastery : stackMastery);
            double normalized = Math.max(0D, Math.min(100D, mastery)) / 100D;
            sum += config.zeroMasteryMulti + (1D - config.zeroMasteryMulti) * normalized;
            count++;
        }
        if (config.currentHealthInfluence) {
            sum += dmzrevamp$resourceRatio(player.getHealth(), player.getMaxHealth(), config.zeroHealthMulti);
            count++;
        }
        if (config.currentKiInfluence) {
            sum += dmzrevamp$resourceRatio(getResources().getCurrentEnergy(), getMaxEnergy(), config.zeroKiMulti);
            count++;
        }
        if (config.currentStaminaInfluence) {
            sum += dmzrevamp$resourceRatio(getResources().getCurrentStamina(), getMaxStamina(), config.zeroStaminaInfluence);
            count++;
        }
        return count == 0 ? 1D : sum / count;
    }

    private static double dmzrevamp$resourceRatio(double current, double maximum, double zeroValue) {
        double ratio = maximum > 0D ? Math.max(0D, Math.min(1D, current / maximum)) : 1D;
        return zeroValue + (1D - zeroValue) * ratio;
    }
}
