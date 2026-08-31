package com.dmzrevamp.mixin;

import com.dmzrevamp.config.KiClashConfigured;
import com.dragonminez.common.combat.clash.BeamClash;
import com.dragonminez.common.combat.clash.ClashParticipant;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BeamClash.class)
public abstract class BeamClashConfiguredMixin {
    @Shadow @Final private ClashParticipant a;
    @Shadow @Final private ClashParticipant b;

    @ModifyConstant(method = "tick", constant = @Constant(intValue = 100), remap = false)
    private int dmzrevamp$neverDissolveFromIdle(int original) { return Integer.MAX_VALUE; }

    @ModifyConstant(method = "tick", constant = @Constant(intValue = 600), remap = false)
    private int dmzrevamp$maxDuration(int original) { return KiClashConfigured.get().maxClashDurationTicks; }

    @ModifyConstant(method = "tick", constant = @Constant(floatValue = 0.8F), remap = false)
    private float dmzrevamp$advantageHigh(float original) { return KiClashConfigured.get().innerAdvantageHigh; }

    @ModifyConstant(method = "tick", constant = @Constant(floatValue = 0.19999999F), remap = false)
    private float dmzrevamp$advantageLow(float original) { return KiClashConfigured.get().innerAdvantageLow; }

    @Inject(method = "resolve", at = @At("HEAD"), remap = false)
    private void dmzrevamp$immobilizeLoser(BeamClash.Result result, CallbackInfo ci) {
        ClashParticipant loser = result == BeamClash.Result.A_WINS ? b : result == BeamClash.Result.B_WINS ? a : null;
        ClashParticipant winner = result == BeamClash.Result.A_WINS ? a : result == BeamClash.Result.B_WINS ? b : null;
        if (winner != null) com.dmzrevamp.revamp.ki.KiClashTeams.applyWinningHelperDamage((BeamClash) (Object) this, winner);
        if (winner != null) com.dmzrevamp.revamp.ki.KiClashTeams.releaseMovement(winner.owner());
        if (loser != null && loser.owner().isAlive()) com.dmzrevamp.revamp.ki.KiClashTeams.immobilizeAfterLoss(loser.owner(), 40);
    }
}
