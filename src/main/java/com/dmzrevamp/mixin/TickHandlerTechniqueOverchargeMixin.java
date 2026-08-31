package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.classes.skills.ClassSkillEvents;
import com.dmzrevamp.revamp.ki.KiAttackOverhaul;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Resources;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.techniques.TechniqueData;
import com.dragonminez.common.stats.techniques.Techniques;
import com.dragonminez.server.events.players.TickHandler;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = TickHandler.class, priority = 900)
public abstract class TickHandlerTechniqueOverchargeMixin {
    @ModifyConstant(method = "handleTechniqueCharge", constant = @Constant(floatValue = 175.0F), remap = false)
    // Extends the held charge target from DMZ's 175 percent cap to 400 percent.
    private static float dmzrevamp$extendHeldChargeTarget(float original) {
        return KiAttackOverhaul.maxChargePercent();
    }

    @ModifyConstant(method = "resolveKiAttackOnRelease", constant = @Constant(floatValue = 175.0F), remap = false)
    // Allows released ki attacks to use the full extended charge value.
    private static float dmzrevamp$extendReleasedChargeTarget(float original) {
        return KiAttackOverhaul.maxChargePercent();
    }

    @Redirect(
            method = "handleTechniqueCharge",
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(FF)F", ordinal = 0),
            remap = false
    )
    // Keeps the alpha's charge curve, then halves the charge rate after 175 percent.
    private static float dmzrevamp$slowChargeAfterOriginalCap(float targetPercent, float proposedPercent, ServerPlayer player, StatsData data) {
        return KiAttackOverhaul.extendChargeStep(targetPercent, proposedPercent, player, data);
    }

    @Redirect(
            method = "handleTechniqueCharge",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/stats/techniques/KiAttackData;getCalculatedCost(Lcom/dragonminez/common/stats/StatsData;)D"),
            remap = false
    )
    // Applies class skill ki-cost reductions to continuous technique charge costs.
    private static double dmzrevamp$adjustChargeCost(KiAttackData technique, StatsData data, ServerPlayer player, StatsData methodData) {
        return ClassSkillEvents.adjustKiActionCost(player, data, technique, technique.getCalculatedCost(data));
    }

    @Redirect(
            method = "resolveKiAttackOnRelease",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/stats/techniques/KiAttackData;getCalculatedCost(Lcom/dragonminez/common/stats/StatsData;)D"),
            remap = false
    )
    // Applies class skill ki-cost reductions to the firing half of technique costs.
    private static double dmzrevamp$adjustReleaseCost(KiAttackData technique, StatsData data, ServerPlayer player, StatsData methodData, Techniques techniques) {
        return ClassSkillEvents.adjustKiActionCost(player, data, technique, technique.getCalculatedCost(data));
    }

    @Redirect(
            method = "handleTechniqueCharge",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/stats/character/Resources;getCurrentEnergy()F"),
            remap = false
    )
    // Lets the charge continue when Ki is low by considering Stamina and HP as overflow resources.
    private static float dmzrevamp$getChargePaymentBudget(Resources resources, ServerPlayer player, StatsData data) {
        return KiAttackOverhaul.availableTechniquePayment(resources, player, data);
    }

    @Redirect(
            method = "handleTechniqueCharge",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/stats/character/Resources;removeEnergy(F)V"),
            remap = false
    )
    // Pays continuous charge cost with Ki, then Stamina, then HP.
    private static void dmzrevamp$payChargeCost(Resources resources, float cost, ServerPlayer player, StatsData data) {
        KiAttackOverhaul.payTechniqueCost(resources, cost, player, data);
    }

    @Redirect(
            method = "handleTechniqueCharge",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/stats/character/Resources;setCurrentEnergy(F)V"),
            remap = false
    )
    // Drains the overflow payment pool when the player cannot fully pay a charge tick.
    private static void dmzrevamp$drainChargePaymentBudget(Resources resources, float energy, ServerPlayer player, StatsData data) {
        if (energy <= 0.0F) {
            KiAttackOverhaul.drainAllTechniquePayment(resources, player, data);
        } else {
            resources.setCurrentEnergy(energy);
        }
    }

    @Redirect(
            method = "handleTechniqueCharge",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/stats/techniques/Techniques;setTechniqueChargePercent(F)V"),
            remap = false,
            require = 0
    )
    private static void dmzrevamp$pauseOverchargePercentWhenKiStarved(Techniques techniques, float percent, ServerPlayer player, StatsData data) {
        KiAttackOverhaul.updateChargePercent(data.getResources(), player, data, percent);
    }

    @Redirect(
            method = "resolveKiAttackOnRelease",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/stats/character/Resources;removeEnergy(F)V"),
            remap = false
    )
    // Pays the firing half of the technique cost with overflow resources.
    private static void dmzrevamp$payReleaseCost(Resources resources, float cost, ServerPlayer player, StatsData data, Techniques techniques) {
        KiAttackOverhaul.payReleaseCost(resources, cost, player, data);
    }
}
