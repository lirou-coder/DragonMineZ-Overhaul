package com.dmzrevamp.mixin;

import com.dmzrevamp.racial.CustomRacialActionHelper;
import com.dmzrevamp.racial.CustomRacialSkill;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.server.events.players.actionmode.RacialModeHandler;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RacialModeHandler.class)
public abstract class RacialModeHandlerCustomSkillMixin {
    @Inject(method = "canCharge", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzrevamp$customRacialCanCharge(ServerPlayer player, StatsData data, CallbackInfoReturnable<Boolean> cir) {
        CustomRacialSkill skill = CustomRacialActionHelper.getCustomRacialSkill(data);
        if (skill != null && skill.showsRacialActionButton(data)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "handleActionCharge", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzrevamp$customRacialCharge(ServerPlayer player, StatsData data, CallbackInfoReturnable<Integer> cir) {
        CustomRacialSkill skill = CustomRacialActionHelper.getCustomRacialSkill(data);
        if (skill == null) {
            return;
        }
        Integer charge = skill.getActionCharge(player, data);
        if (charge != null) {
            cir.setReturnValue(charge);
        }
    }

    @Inject(method = "performAction", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzrevamp$customRacialPerformAction(ServerPlayer player, StatsData data, CallbackInfoReturnable<Boolean> cir) {
        CustomRacialSkill skill = CustomRacialActionHelper.getCustomRacialSkill(data);
        if (skill == null) {
            return;
        }
        Boolean handled = skill.performAction(player, data);
        if (handled != null) {
            cir.setReturnValue(handled);
        }
    }
}
