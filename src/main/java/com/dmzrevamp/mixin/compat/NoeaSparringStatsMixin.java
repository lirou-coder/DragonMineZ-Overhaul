package com.dmzrevamp.mixin.compat;

import com.dmzrevamp.compat.NoeaNpcStatCompat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.butterjaffa.noeabosses.SparringMatchService", remap = false)
public abstract class NoeaSparringStatsMixin {
    @Inject(method = "capturePlayerStats", at = @At("HEAD"), require = 0)
    private static void dmzrevamp$capturePlayerStats(ServerPlayer player, CallbackInfoReturnable<Object> cir) {
        NoeaNpcStatCompat.capturePlayer(player);
    }

    @Inject(
            method = "applyCuratedStats(Lnet/minecraft/world/entity/Mob;Lcom/butterjaffa/noeabosses/SparringRoster$Fighter;DLjava/lang/String;Z)V",
            at = @At("RETURN"),
            require = 0
    )
    private static void dmzrevamp$applyOverhaulTargetStats(Mob mob, @Coerce Object fighter, double scale,
                                                           String difficulty, boolean fullHealth, CallbackInfo ci) {
        NoeaNpcStatCompat.applyTarget(mob, fighter, scale);
    }

    @Inject(
            method = "applyMatchedStats",
            at = @At("RETURN"),
            require = 0
    )
    private static void dmzrevamp$applyOverhaulMatchedStats(Mob mob, @Coerce Object matchedStats,
                                                            boolean fullHealth, @Coerce Object difficulty,
                                                            CallbackInfo ci) {
        NoeaNpcStatCompat.applyMatched(mob, difficulty);
    }
}
