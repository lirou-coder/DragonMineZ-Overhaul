package com.dmzrevamp.mixin;

import com.dmzrevamp.config.AdaptiveDefenseMoreConfigured;
import com.dmzrevamp.revamp.combat.AdaptiveDefenseFullNegationEvents;
import com.dragonminez.common.config.CombatConfig;
import com.dragonminez.server.events.players.combat.CombatEvent;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CombatEvent.class, remap = false)
public abstract class CombatEventAdaptiveDefenseMoreConfiguredMixin {
    @ModifyArg(
            method = "overrideVanillaArmorReduction",
            at = @At(value = "INVOKE", target = "Lnet/minecraftforge/eventbus/api/Event;setCanceled(Z)V"),
            index = 0,
            require = 0,
            remap = false
    )
    private static boolean dmzrevamp$keepFullyNegatedHitConnected(boolean canceled) {
        return false;
    }

    @ModifyArg(
            method = "overrideVanillaArmorReduction",
            at = @At(value = "INVOKE", target = "Lnet/minecraftforge/event/entity/living/LivingDamageEvent;setCanceled(Z)V"),
            index = 0,
            require = 0,
            remap = false
    )
    private static boolean dmzrevamp$keepFullyNegatedLivingDamageConnected(boolean canceled) {
        return false;
    }

    @Inject(method = "applyFullNegation", at = @At("HEAD"), require = 0, remap = false)
    private static void dmzrevamp$notifyFullyNegatedHit(LivingEntity target, CallbackInfo ci) {
        AdaptiveDefenseFullNegationEvents.markFullyNegated(target);
    }

    @Redirect(
            method = "overrideVanillaArmorReduction",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dragonminez/common/config/CombatConfig;getCancelDamageEventIfMitigationTooHigh()Z"
            ),
            require = 0
    )
    private static boolean dmzrevamp$honorConfiguredFullNegation(CombatConfig originalConfig) {
        return AdaptiveDefenseMoreConfigured.get().adaptiveDefense.enabled
                || originalConfig.getCancelDamageEventIfMitigationTooHigh();
    }
}
