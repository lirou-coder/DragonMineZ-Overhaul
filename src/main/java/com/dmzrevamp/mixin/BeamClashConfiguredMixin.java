package com.dmzrevamp.mixin;

import com.dmzrevamp.config.KiClashConfigured;
import com.dmzrevamp.network.DmzRevampNetwork;
import com.dmzrevamp.network.KiClashMeterConfigS2CPacket;
import com.dmzrevamp.revamp.ki.KiClashTeams;
import com.dragonminez.common.combat.clash.BeamClash;
import com.dragonminez.common.combat.clash.ClashParticipant;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BeamClash.class)
public abstract class BeamClashConfiguredMixin {
    @Shadow @Final private ClashParticipant a;
    @Shadow @Final private ClashParticipant b;

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void dmzrevamp$syncMeterConfiguration(ClashParticipant first, ClashParticipant second,
                                                  long startGameTime, CallbackInfo ci) {
        KiClashConfigured.Config config = KiClashConfigured.get();
        KiClashMeterConfigS2CPacket packet = new KiClashMeterConfigS2CPacket(
                config.meterSpeedMultiplier, config.goodAreaSizeMultiplier,
                config.perfectAreaFraction, config.goodMinimumEfficiency);
        if (first.owner() instanceof ServerPlayer player) {
            DmzRevampNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
        if (second.owner() instanceof ServerPlayer player) {
            DmzRevampNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }

    /** Overhaul intentionally lets an inactive contest survive until maxClashDurationTicks. */
    @ModifyConstant(method = "tick", constant = @Constant(intValue = 140), remap = false)
    private int dmzrevamp$neverDissolveFromIdle(int original) { return Integer.MAX_VALUE; }

    @ModifyConstant(method = "tick", constant = @Constant(intValue = 600), remap = false)
    private int dmzrevamp$maxDuration(int original) { return KiClashConfigured.get().maxClashDurationTicks; }

    @ModifyConstant(method = "tick", constant = @Constant(floatValue = 0.8F), remap = false)
    private float dmzrevamp$advantageHigh(float original) { return KiClashConfigured.get().innerAdvantageHigh; }

    @ModifyConstant(method = "tick", constant = @Constant(floatValue = 0.19999999F), remap = false)
    private float dmzrevamp$advantageLow(float original) { return KiClashConfigured.get().innerAdvantageLow; }

    /**
     * DMZ 2.2 derives lock length and client advantage from visualBias(). If the Overhaul changes
     * the win thresholds, the visual mapping has to use the same interval or the clash point/HUD
     * reaches 0/100% before (or after) the actual win condition.
     */
    @Inject(method = "visualBias", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dmzrevamp$configuredVisualBias(float bias, CallbackInfoReturnable<Float> cir) {
        float low = KiClashConfigured.get().innerAdvantageLow;
        float high = KiClashConfigured.get().innerAdvantageHigh;
        float span = Math.max(0.0001F, high - low);
        cir.setReturnValue(Mth.clamp((bias - low) / span, 0.0F, 1.0F));
    }

    /**
     * Overhaul applies live Ki Damage/overcharge influence when momentum is earned. Neutralize only
     * DMZ's second stat-power multiplier in BeamClash.tick, while retaining the real statPower in
     * ClashParticipant so DMZ 2.2's new NPC accuracy calculation still works.
     */
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/combat/clash/ClashParticipant;statPower()D"), remap = false)
    private double dmzrevamp$neutralizeDuplicateNativePower(ClashParticipant participant) {
        return 1.0D;
    }

    @Inject(method = "resolve", at = @At("HEAD"), remap = false)
    private void dmzrevamp$prepareOverhaulResolution(BeamClash.Result result, CallbackInfo ci) {
        ClashParticipant loser = result == BeamClash.Result.A_WINS ? b : result == BeamClash.Result.B_WINS ? a : null;
        ClashParticipant winner = result == BeamClash.Result.A_WINS ? a : result == BeamClash.Result.B_WINS ? b : null;
        if (winner != null) KiClashTeams.applyWinningHelperDamage((BeamClash) (Object) this, winner);
        if (loser != null && loser.owner().isAlive()) {
            // DMZ 2.2 itself applies STUN for exactly LOSER_EXHAUST_TICKS in BeamClash.resolve().
            // We only remember that native window so packet guards cover actions whose base packet
            // forgot to test stun; no second movement lock or custom stun is created here.
            KiClashTeams.markNativeExhaustGuard(loser.owner(), BeamClash.LOSER_EXHAUST_TICKS);
        }
    }
}
