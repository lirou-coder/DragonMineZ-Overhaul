package com.dmzrevamp.mixin.client;

import com.dmzrevamp.client.DmzRevampClientHooks;
import com.dmzrevamp.config.DmzRevampConfig;
import com.dmzrevamp.revamp.DmzSpeedRevampEvents;
import com.dragonminez.client.events.FlySkillEvent;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FlySkillEvent.class)
public abstract class FlySkillEventRevampSpeedMixin {
    @Inject(method = "getFlySpeedScale", at = @At("RETURN"), cancellable = true, remap = false)
    private static void dmzrevamp$applyRevampSearchFlightSpeed(LocalPlayer player, CallbackInfoReturnable<Float> cir) {
        if (!DmzRevampConfig.ENABLE_SPD_MOVEMENT_SPEED_MODIFIERS.get()) {
            return;
        }
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            if (!data.getStatus().isHasCreatedCharacter()) {
                return;
            }
            double revampScale = DmzSpeedRevampEvents.getActiveSearchFlightSpeedMultiplier(
                    player,
                    data,
                    DmzRevampClientHooks.getLocalFlightMovementTicks()
            );
            cir.setReturnValue((float) Math.max(cir.getReturnValue(), revampScale));
        });
    }
}
