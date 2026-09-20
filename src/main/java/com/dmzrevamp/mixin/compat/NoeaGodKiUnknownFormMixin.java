package com.dmzrevamp.mixin.compat;

import com.dmzrevamp.compat.NoeaCompat;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps God Ki's generic mechanics/visuals on addon forms absent from Noea's fixed ledger. */
@Mixin(targets = "com.butterjaffa.noeabosses.GodKiEnhancementRules", remap = false)
public abstract class NoeaGodKiUnknownFormMixin {
    @Redirect(
            method = "enforceCompatibility",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/butterjaffa/noeabosses/GodKiVariantLedger;find(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Lcom/butterjaffa/noeabosses/GodKiVariantLedger$Entry;"
            ),
            remap = false
    )
    @Coerce
    private static Object dmzrevamp$useCanonicalOverhaulFormForCompatibility(
            String race, String group, String form) {
        return NoeaCompat.findGodKiVariant(race, group, form);
    }

    @Inject(method = "clearEnhancement", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dmzrevamp$keepGenericEnhancementForUnknownForms(
            ServerPlayer player, StatsData data, String reason, CallbackInfo ci) {
        if ("message.noeabosses.god_ki_enhancement.unsupported".equals(reason)) {
            ci.cancel();
        }
    }
}
