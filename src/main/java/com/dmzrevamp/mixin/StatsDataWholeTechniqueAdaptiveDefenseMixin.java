package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.combat.AdaptiveDefenseDamageContext;
import com.dragonminez.common.stats.StatsData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayDeque;

/** Feeds DMZ's own adaptive-defense curve the full technique damage for multi-hit attacks. */
@Mixin(value = StatsData.class, remap = false)
public abstract class StatsDataWholeTechniqueAdaptiveDefenseMixin {
    @Unique
    private static final ThreadLocal<ArrayDeque<Double>> DMZREVAMP_DAMAGE_STACK = ThreadLocal.withInitial(ArrayDeque::new);

    @Inject(method = "calculatePostMitigationDamage", at = @At("HEAD"), remap = false, require = 0)
    private void dmzrevamp$pushIncomingDamage(double incomingDamage, boolean isGuardBroken,
                                               double armorPenetration, CallbackInfoReturnable<Double> cir) {
        DMZREVAMP_DAMAGE_STACK.get().push(incomingDamage);
    }

    @ModifyArg(
            method = "calculatePostMitigationDamage",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/stats/StatsData;computeAdaptativeDefenseMitigation(D)D"),
            index = 0,
            remap = false,
            require = 0
    )
    private double dmzrevamp$useWholeTechniqueDamageForNativeMitigation(double nativeRatio) {
        AdaptiveDefenseDamageContext.Entry context = AdaptiveDefenseDamageContext.current();
        ArrayDeque<Double> damageStack = DMZREVAMP_DAMAGE_STACK.get();
        if (context == null || damageStack.isEmpty()) return nativeRatio;

        double hitDamage = damageStack.peek();
        if (!Double.isFinite(hitDamage) || hitDamage <= 0D
                || !Double.isFinite(context.totalTechniqueDamage())) {
            return nativeRatio;
        }
        return nativeRatio * (Math.max(hitDamage, context.totalTechniqueDamage()) / hitDamage);
    }

    @Inject(method = "calculatePostMitigationDamage", at = @At("RETURN"), remap = false, require = 0)
    private void dmzrevamp$popIncomingDamage(double incomingDamage, boolean isGuardBroken,
                                              double armorPenetration, CallbackInfoReturnable<Double> cir) {
        ArrayDeque<Double> damageStack = DMZREVAMP_DAMAGE_STACK.get();
        if (!damageStack.isEmpty()) damageStack.pop();
        if (damageStack.isEmpty()) DMZREVAMP_DAMAGE_STACK.remove();
    }
}
