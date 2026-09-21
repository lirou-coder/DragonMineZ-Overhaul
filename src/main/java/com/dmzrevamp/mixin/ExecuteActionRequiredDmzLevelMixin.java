package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.forms.RequiredDmzLevelGuard;
import com.dragonminez.common.network.C2S.ExecuteActionC2S;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.util.TransformationsHelper;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Applies the normal form level requirement to DMZ's double-tap instant transformation path. */
@Mixin(value = ExecuteActionC2S.class, remap = false)
public abstract class ExecuteActionRequiredDmzLevelMixin {
    @Inject(method = "instantTransformForm", at = @At("HEAD"), cancellable = true)
    private static void dmzrevamp$denyInstantFormBelowRequiredLevel(ServerPlayer player, StatsData data,
                                                                    CallbackInfoReturnable<Boolean> cir) {
        var form = TransformationsHelper.getNextAvailableForm(data);
        if (form != null && !RequiredDmzLevelGuard.allows(player, data, form)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "instantTransformStackForm", at = @At("HEAD"), cancellable = true)
    private static void dmzrevamp$denyInstantStackFormBelowRequiredLevel(ServerPlayer player, StatsData data,
                                                                         CallbackInfoReturnable<Boolean> cir) {
        var form = TransformationsHelper.getNextAvailableStackForm(data);
        if (form != null && !RequiredDmzLevelGuard.allows(player, data, form)) {
            cir.setReturnValue(false);
        }
    }
}
