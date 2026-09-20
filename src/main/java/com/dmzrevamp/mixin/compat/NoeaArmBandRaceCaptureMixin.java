package com.dmzrevamp.mixin.compat;

import com.dmzrevamp.compat.NoeaCompat;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.butterjaffa.noeabosses.ArmBandFusionBridge", remap = false)
public abstract class NoeaArmBandRaceCaptureMixin {
    @Inject(method = "prepare", at = @At("HEAD"), remap = false)
    private static void dmzrevamp$captureRealRaces(ServerPlayer player, ServerPlayer partner,
                                                    StatsData data, StatsData partnerData, CallbackInfo ci) {
        NoeaCompat.captureRealFusionRaces(data, partnerData);
    }

    @Inject(method = "restoreOriginalRaces", at = @At("RETURN"), remap = false)
    private static void dmzrevamp$clearRealRaces(ServerPlayer player, CallbackInfo ci) {
        NoeaCompat.clearRealFusionRaces();
    }
}
