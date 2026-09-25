package com.dmzrevamp.mixin;

import com.dmzrevamp.config.KiClashConfigured;
import com.dmzrevamp.revamp.ki.KiClashTeams;
import com.dragonminez.common.combat.clash.ClashParticipant;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClashParticipant.class)
public abstract class ClashParticipantConfiguredMixin implements com.dmzrevamp.revamp.ki.ClashParticipantAccess {
    @Accessor("momentum")
    public abstract float dmzrevamp$getMomentum();

    @Accessor("momentum")
    public abstract void dmzrevamp$setMomentum(float momentum);

    /** Share every native 2.2 GOOD/PERFECT/NPC burst with the whole Overhaul team. */
    @Inject(method = "addBurst", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzrevamp$shareTeamBurst(float efficiency, CallbackInfo ci) {
        KiClashTeams.applyMomentumBurst((ClashParticipant) (Object) this, efficiency);
        ci.cancel();
    }

    /** Keep Overhaul's configurable decay/helper reduction while targeting DMZ 2.2's 0.94 baseline. */
    @ModifyConstant(method = "tickMeter", constant = @Constant(floatValue = 0.94F), remap = false)
    private float dmzrevamp$momentumDecay(float original) {
        return KiClashTeams.adjustedMomentumDecay((ClashParticipant) (Object) this,
                KiClashConfigured.get().momentumDecayPerTick);
    }
}
