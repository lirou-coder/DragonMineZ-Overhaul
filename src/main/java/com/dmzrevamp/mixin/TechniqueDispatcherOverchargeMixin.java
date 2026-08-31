package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.ki.KiAttackOverhaul;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.techniques.TechniqueDispatcher;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TechniqueDispatcher.class)
public abstract class TechniqueDispatcherOverchargeMixin {
    @ModifyConstant(method = "executeKiAttack", constant = @Constant(floatValue = 2.0F), remap = false)
    // Extends the final ki attack power multiplier from 200 percent to 400 percent.
    private static float dmzrevamp$extendEffectiveChargeMultiplier(float original) {
        return KiAttackOverhaul.maxChargePercent() / 100.0F;
    }

    @Redirect(
            method = "executeKiAttack",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/stats/techniques/KiAttackData;getSize()F"),
            require = 0,
            remap = false
    )
    // Doubles projectile size gradually between 175 percent and 400 percent charge.
    private static float dmzrevamp$scaleTechniqueSize(KiAttackData technique, LivingEntity caster, Level level, KiAttackData methodTechnique, StatsData data, float chargeMultiplier) {
        return technique.getSize() * KiAttackOverhaul.projectileSizeMultiplier(chargeMultiplier);
    }

}
