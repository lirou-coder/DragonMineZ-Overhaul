package com.dmzrevamp.mixin;

import com.dmzrevamp.config.KiClashConfigured;
import com.dmzrevamp.revamp.ki.KiClashTransformationBridge;
import com.dragonminez.common.combat.clash.BeamClashManager;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Status;
import com.dragonminez.server.events.players.TickHandler;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(value = TickHandler.class, remap = false)
public abstract class TickHandlerClashTransformationMixin {
    @Redirect(method = "lambda$onPlayerTick$1", at = @At(value = "INVOKE",
            target = "Lcom/dragonminez/common/stats/character/Status;isStrikeLocked()Z"), remap = false, require = 1)
    private static boolean dmzrevamp$allowChargeDuringStrikeClash(Status status, ServerPlayer player,
                                                                   int tickCounter, UUID playerId, StatsData data) {
        if (player != null && KiClashConfigured.get().allowTransformationMidClash
                && BeamClashManager.isClashing(player.getUUID())) {
            // Keep the clash's actual strike lock intact; only prevent TickHandler
            // from treating it as a stun while processing Ki/action charge input.
            return false;
        }
        return status.isStrikeLocked();
    }

    @Inject(method = "handleActionCharge", at = @At("HEAD"))
    private static void dmzrevamp$restoreChargedTransformationInput(ServerPlayer player, StatsData data,
                                                                    CallbackInfo ci) {
        KiClashTransformationBridge.restoreActionCharge(player, data);
    }
}
