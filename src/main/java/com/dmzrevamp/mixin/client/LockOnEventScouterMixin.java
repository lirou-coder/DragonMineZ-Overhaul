package com.dmzrevamp.mixin.client;

import com.dmzrevamp.client.LockOnCycleClientEvents;
import com.dmzrevamp.client.ScouterClientState;
import com.dragonminez.client.events.LockOnEvent;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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

    @Inject(method = "onClientTick", at = @At("HEAD"), cancellable = true, require = 0)
    private static void dmzrevamp$keepScouterBackedLockWithoutKiSense(TickEvent.ClientTickEvent event, CallbackInfo ci) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = Minecraft.getInstance().player;
        if (player == null || LockOnEventAccessor.dmzrevamp$getLockedTarget() == null) {
            return;
        }
        StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
        if (data != null && ScouterClientState.validateScouterLock(player, data)) {
            ci.cancel();
        }
    }
}
