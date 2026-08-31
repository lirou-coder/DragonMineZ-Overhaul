package com.dmzrevamp.mixin.client;

import com.dmzrevamp.client.ScouterClientState;
import com.dmzrevamp.client.LockOnCycleClientEvents;
import com.dragonminez.client.events.LockOnEvent;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Optional;

@Mixin(value = LockOnEvent.class, remap = false)
public abstract class LockOnEventScouterMixin {
    @Inject(method = "findTargetInFront", at = @At("HEAD"), cancellable = true, require = 0)
    private static void dmzrevamp$selectClosestToCrosshair(Player player, double range, StatsData data,
                                                           CallbackInfoReturnable<Optional<LivingEntity>> cir) {
        Optional<LivingEntity> target = LockOnCycleClientEvents.findPrioritizedTargets(player, range, data)
                .stream().findFirst();
        target.ifPresent(LockOnCycleClientEvents::rememberTarget);
        cir.setReturnValue(target);
    }

    @Inject(method = "unlock", at = @At("HEAD"), require = 0)
    private static void dmzrevamp$clearLockCycleHistory(CallbackInfo ci) {
        LockOnCycleClientEvents.clearHistory();
    }

    @Redirect(method = "lambda$onClientTick$2",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/client/systems/kisense/KiSenseScan;canTarget(Lnet/minecraft/world/entity/LivingEntity;Lcom/dragonminez/common/stats/StatsData;)Z"),
            require = 0)
    private static boolean dmzrevamp$allowAndroidPlayerLockOn(LivingEntity target, StatsData data) {
        return LockOnCycleClientEvents.canTarget(target, data);
    }


    @Inject(method = "lambda$onClientTick$2", at = @At("HEAD"), cancellable = true, require = 0)
    private static void dmzrevamp$keepScouterBackedLockWithoutKiSense(Player player, StatsData data, CallbackInfo ci) {
        if (ScouterClientState.validateScouterLock(player, data)) {
            ci.cancel();
        }
    }
}
