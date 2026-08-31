package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.DmzSpeedRevampEvents;
import com.dmzrevamp.revamp.ki.KiClashTeams;
import com.dragonminez.common.network.C2S.CombatAttackRequestC2S;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CombatAttackRequestC2S.class)
public abstract class CombatAttackRequestSprintGraceMixin {
    @Inject(method = "processAttackRequest", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dmzrevamp$markEmptyAttackSprintGrace(ServerPlayer player, CombatAttackRequestC2S request, CallbackInfo ci) {
        if (KiClashTeams.isAbilityRestricted(player)) {
            ci.cancel();
            return;
        }
        if (request.getEntityIds().length == 0) {
            player.getPersistentData().putInt(
                    DmzSpeedRevampEvents.EMPTY_ATTACK_SPRINT_GRACE_TAG,
                    DmzSpeedRevampEvents.EMPTY_ATTACK_SPRINT_GRACE_TICKS
            );
        } else {
            player.getPersistentData().remove(DmzSpeedRevampEvents.EMPTY_ATTACK_SPRINT_GRACE_TAG);
        }
    }
}
