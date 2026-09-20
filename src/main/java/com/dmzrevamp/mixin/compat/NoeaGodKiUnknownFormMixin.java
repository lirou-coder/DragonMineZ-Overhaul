package com.dmzrevamp.mixin.compat;

import com.butterjaffa.noeabosses.GodKiVariantLedger;
import com.dmzrevamp.compat.NoeaCompat;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
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
    private static GodKiVariantLedger.Entry dmzrevamp$useCanonicalOverhaulFormForCompatibility(
            String race, String group, String form) {
        String[] alias = NoeaCompat.aliasedGodKiVariantIds(race, group, form);
        return alias == null
                ? GodKiVariantLedger.find(race, group, form)
                : GodKiVariantLedger.find(alias[0], alias[1], alias[2]);
    }

    @Inject(method = "clearEnhancement", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dmzrevamp$keepGenericEnhancementForUnknownForms(
            ServerPlayer player, StatsData data, String reason, CallbackInfo ci) {
        if ("message.noeabosses.god_ki_enhancement.unsupported".equals(reason)) {
            ci.cancel();
        }
    }
}
