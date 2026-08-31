package com.dmzrevamp.mixin;

import com.dmzrevamp.config.KiClashConfigured;
import com.dmzrevamp.revamp.ki.KiClashTeams;
import com.dragonminez.common.combat.clash.ClashParticipant;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClashParticipant.class)
public abstract class ClashParticipantConfiguredMixin implements com.dmzrevamp.revamp.ki.ClashParticipantAccess {
    @Accessor("momentum")
    public abstract float dmzrevamp$getMomentum();

    @Accessor("momentum")
    public abstract void dmzrevamp$setMomentum(float momentum);

    @Inject(method = "addBurst", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzrevamp$shareTeamBurst(float efficiency, CallbackInfo ci) {
        KiClashTeams.applyMomentumBurst((ClashParticipant) (Object) this, efficiency);
        ci.cancel();
    }
    @ModifyConstant(method = "tickMeter", constant = @Constant(floatValue = 0.01F), remap = false)
    private float dmzrevamp$meterSpeed(float original) { return KiClashConfigured.get().meterSpeedPerTick; }

    @ModifyConstant(method = "tickMeter", constant = @Constant(floatValue = 0.96F), remap = false)
    private float dmzrevamp$momentumDecay(float original) {
        return KiClashTeams.adjustedMomentumDecay((ClashParticipant) (Object) this, KiClashConfigured.get().momentumDecayPerTick);
    }

    @ModifyConstant(method = "botConsistencyPenalty", constant = @Constant(floatValue = 0.78F), remap = false)
    private static float dmzrevamp$goodLow(float original) { return KiClashConfigured.get().goodAreaLow; }

    @ModifyConstant(method = "botConsistencyPenalty", constant = @Constant(floatValue = 0.96F), remap = false)
    private static float dmzrevamp$goodHigh(float original) { return KiClashConfigured.get().goodAreaHigh; }

    @Inject(method = "scoreEfficiency", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dmzrevamp$configuredScore(float phase, CallbackInfoReturnable<Float> cir) {
        var config = KiClashConfigured.get();
        if (phase < config.goodAreaLow || phase > config.goodAreaHigh) {
            cir.setReturnValue(config.offWindowMomentumEfficiency);
            return;
        }
        float center = (config.goodAreaLow + config.goodAreaHigh) * 0.5F;
        float half = Math.max(0.0001F, (config.goodAreaHigh - config.goodAreaLow) * 0.5F);
        cir.setReturnValue(config.offWindowMomentumEfficiency
                + (1F - config.offWindowMomentumEfficiency) * Math.max(0F, 1F - Math.abs(phase - center) / half));
    }
}
