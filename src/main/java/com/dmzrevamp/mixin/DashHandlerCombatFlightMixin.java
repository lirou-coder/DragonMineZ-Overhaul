package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.combat.CombatFlightDashHandler;
import com.dmzrevamp.revamp.growth.DynamicGrowthRevampEvents;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Status;
import com.dragonminez.server.events.players.combat.DashHandler;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DashHandler.class)
public abstract class DashHandlerCombatFlightMixin {
    @Inject(method = "lambda$handleDash$0", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dmzrevamp$handleCombatFlightDoubleDash(ServerPlayer player, boolean doubleDash, float zInput, float xInput, StatsData data, CallbackInfo ci) {
        if (doubleDash && CombatFlightDashHandler.tryHandleDoubleDash(player, zInput, xInput, data)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "lambda$handleDash$0",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/util/ComboManager;consumeTeleport(Ljava/util/UUID;)V"),
            remap = false
    )
    private static void dmzrevamp$awardPerfectCounterGrowth(ServerPlayer player, boolean doubleDash, float zInput, float xInput, StatsData data, CallbackInfo ci) {
        DynamicGrowthRevampEvents.awardPerfectCounter(player);
    }

    @Redirect(
            method = "lambda$handleDash$0",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/stats/character/Status;getFlightMode()I"),
            remap = false
    )
    private static int dmzrevamp$allowCombatFlightDash(Status status) {
        return Status.FLIGHT_SEARCH;
    }

    @Inject(
            method = "lambda$handleDash$0",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dragonminez/common/network/S2C/StatsSyncS2C;<init>(Lnet/minecraft/server/level/ServerPlayer;)V"
            ),
            remap = false
    )
    private static void dmzrevamp$notifySuccessfulCombatFlightDash(
            ServerPlayer player,
            boolean doubleDash,
            float zInput,
            float xInput,
            StatsData data,
            CallbackInfo ci
    ) {
        CombatFlightDashHandler.notifyClientImpulse(player, xInput, zInput, data);
    }
}
