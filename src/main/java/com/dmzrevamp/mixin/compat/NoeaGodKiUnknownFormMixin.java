package com.dmzrevamp.mixin.compat;

import com.dragonminez.common.stats.StatsData;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps God Ki's generic mechanics/visuals on addon forms absent from Noea's fixed ledger. */
@Mixin(targets = "com.butterjaffa.noeabosses.GodKiEnhancementRules", remap = false)
public abstract class NoeaGodKiUnknownFormMixin {
    @Inject(method = "clearEnhancement", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dmzrevamp$keepGenericEnhancementForUnknownForms(
            ServerPlayer player, StatsData data, String reason, CallbackInfo ci) {
        if ("message.noeabosses.god_ki_enhancement.unsupported".equals(reason)) {
            ci.cancel();
        }
    }
}
