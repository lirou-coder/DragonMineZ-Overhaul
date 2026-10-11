package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.ki.KiClashTeams;
import com.dmzrevamp.revamp.ki.KiClashAttackResolver;
import com.dmzrevamp.config.KiClashConfigured;
import com.dmzrevamp.config.StrikeClashConfigured;
import com.dmzrevamp.revamp.strike.StrikeClashManager;
import com.dmzrevamp.revamp.strike.ClashParticipantPowerAccess;
import com.dragonminez.common.combat.clash.ClashParticipant;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClashParticipant.class)
public abstract class ClashParticipantConfiguredMixin implements com.dmzrevamp.revamp.ki.ClashParticipantAccess, ClashParticipantPowerAccess {
    @Accessor("momentum")
    public abstract float dmzrevamp$getMomentum();

    @Accessor("momentum")
    public abstract void dmzrevamp$setMomentum(float momentum);

    @Accessor("statPower")
    @org.spongepowered.asm.mixin.Mutable
    @Override
    public abstract void dmzrevamp$setStatPower(double power);

    /** Share every native 2.2 GOOD/PERFECT/NPC burst with the whole Overhaul team. */
    @Inject(method = "addBurst", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzrevamp$shareTeamBurst(float efficiency, CallbackInfo ci) {
        if (((ClashParticipant) (Object) this).beam() == null) return;
        KiClashTeams.applyMomentumBurst((ClashParticipant) (Object) this, efficiency);
        ci.cancel();
    }

    /** Keep Overhaul's configurable decay/helper reduction while targeting DMZ 2.2's 0.94 baseline. */
    @ModifyConstant(method = "tickMeter", constant = @Constant(floatValue = 0.94F), remap = false)
    private float dmzrevamp$momentumDecay(float original) {
        ClashParticipant participant = (ClashParticipant) (Object) this;
        return participant.beam() == null
                ? StrikeClashConfigured.get().momentumDecayPerTick
                : KiClashTeams.adjustedMomentumDecay(participant, KiClashConfigured.get().momentumDecayPerTick);
    }

    @ModifyConstant(method = "addBurst", constant = @Constant(floatValue = 1.0F), remap = false)
    private float dmzrevamp$strikeBurstMultiplier(float original) {
        return ((ClashParticipant) (Object) this).beam() == null
                ? original * StrikeClashConfigured.get().momentumGainDefaultMultiplier : original;
    }

    @Inject(method = "<init>(Lnet/minecraft/world/entity/LivingEntity;D)V", at = @At("RETURN"), remap = false)
    private void dmzrevamp$registerNativeStrikeParticipant(net.minecraft.world.entity.LivingEntity owner,
                                                            double power, CallbackInfo ci) {
        StrikeClashManager.registerNativeParticipant((ClashParticipant) (Object) this);
    }

    /**
     * Configured non-beam attacks (notably Explosion) have no native MAJOR
     * role. Keep them alive for the complete firing window instead of letting
     * DMZ dissolve their clash on its first participant tick.
     */
    @Redirect(method = "isStillFiring", at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/init/entities/ki/AbstractKiProjectile;isClashableBeam()Z"), remap = false)
    private boolean dmzrevamp$configuredAttackStillActive(AbstractKiProjectile projectile) {
        return projectile.isClashableBeam()
                || (KiClashAttackResolver.isAllowed(projectile) && KiClashAttackResolver.isLaunched(projectile));
    }
}
