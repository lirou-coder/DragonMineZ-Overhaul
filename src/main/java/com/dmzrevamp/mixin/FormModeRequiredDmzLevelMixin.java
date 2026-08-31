package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.forms.RequiredDmzLevelGuard;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.util.TransformationsHelper;
import com.dragonminez.server.events.players.actionmode.FormModeHandler;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FormModeHandler.class, remap = false)
public abstract class FormModeRequiredDmzLevelMixin {
    @Inject(method = "canCharge", at = @At("HEAD"), cancellable = true)
    private void dmzrevamp$denyCharge(ServerPlayer player, StatsData data, CallbackInfoReturnable<Boolean> cir) {
        var form = TransformationsHelper.getNextAvailableForm(data);
        if (form != null && !RequiredDmzLevelGuard.allows(player, data, form)) cir.setReturnValue(false);
    }

    @Inject(method = "attemptTransform", at = @At("HEAD"), cancellable = true)
    private static void dmzrevamp$denyCompletion(ServerPlayer player, StatsData data, CallbackInfo ci) {
        var form = TransformationsHelper.getNextAvailableForm(data);
        if (form != null && !RequiredDmzLevelGuard.allows(player, data, form)) ci.cancel();
    }
}
