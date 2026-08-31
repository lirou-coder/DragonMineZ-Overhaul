package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.ki.KiClashTransformationBridge;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.server.events.players.TickHandler;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TickHandler.class, remap = false)
public abstract class TickHandlerClashTransformationMixin {
    @Inject(method = "handleActionCharge", at = @At("HEAD"))
    private static void dmzrevamp$restoreChargedTransformationInput(ServerPlayer player, StatsData data,
                                                                    CallbackInfo ci) {
        KiClashTransformationBridge.restoreActionCharge(player, data);
    }
}
