package com.dmzrevamp.mixin.compat;

import com.dmzrevamp.compat.NoeaCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Maps reorganized Overhaul forms onto the equivalent visual entries in Noea's fixed ledger. */
@Mixin(targets = "com.butterjaffa.noeabosses.GodKiVariantLedger", remap = false)
public abstract class NoeaGodKiVariantAliasMixin {
    @Inject(method = "find", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dmzrevamp$resolveOverhaulFormAlias(
            String race, String group, String form, CallbackInfoReturnable<Object> cir) {
        Object aliased = NoeaCompat.findAliasedGodKiVariant(race, group, form);
        if (aliased != null) cir.setReturnValue(aliased);
    }
}
