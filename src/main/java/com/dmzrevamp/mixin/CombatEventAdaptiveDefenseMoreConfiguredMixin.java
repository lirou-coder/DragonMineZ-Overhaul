package com.dmzrevamp.mixin;

import com.dmzrevamp.config.AdaptiveDefenseMoreConfigured;
import com.dragonminez.common.config.CombatConfig;
import com.dragonminez.server.events.players.combat.CombatEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = CombatEvent.class, remap = false)
public abstract class CombatEventAdaptiveDefenseMoreConfiguredMixin {
    @Redirect(
            method = "overrideVanillaArmorReduction",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dragonminez/common/config/CombatConfig;getCancelDamageEventIfMitigationTooHigh()Z"
            ),
            require = 0
    )
    private static boolean dmzrevamp$honorConfiguredFullNegation(CombatConfig originalConfig) {
        return AdaptiveDefenseMoreConfigured.get().enable
                || originalConfig.getCancelDamageEventIfMitigationTooHigh();
    }
}
