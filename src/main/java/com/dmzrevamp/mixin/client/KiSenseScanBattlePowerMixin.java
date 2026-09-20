package com.dmzrevamp.mixin.client;

import com.dmzrevamp.revamp.battlepower.ManualBattlePowerStatEvents;
import com.dmzrevamp.revamp.battlepower.AccurateMobBattlePowerCalculator;
import com.dmzrevamp.config.KiSenseBlacklistConfig;
import com.dragonminez.client.systems.kisense.KiSenseScan;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = KiSenseScan.class, remap = false)
public abstract class KiSenseScanBattlePowerMixin {
    @Inject(method = "canTarget", at = @At("HEAD"), cancellable = true, require = 0)
    private static void dmzrevamp$filterBlacklistedTargets(LivingEntity target,
                                                           com.dragonminez.common.stats.StatsData data,
                                                           CallbackInfoReturnable<Boolean> cir) {
        if (KiSenseBlacklistConfig.contains(target)) cir.setReturnValue(false);
    }

    @Inject(method = "getEntityBP", at = @At("HEAD"), cancellable = true, require = 0)
    private static void dmzrevamp$useLongManualBattlePower(LivingEntity entity, CallbackInfoReturnable<Float> cir) {
        if (entity instanceof Player) {
            return;
        }

        if (ManualBattlePowerStatEvents.isKiSenseHiddenEntity(entity)) {
            cir.setReturnValue(Float.MAX_VALUE);
            return;
        }

        // Match the player path: calculate in double and expose only the final scan value as
        // float. Never consult IBattlePower here because DMZ stores mob BP as a capped int.
        double battlePower = AccurateMobBattlePowerCalculator.calculateCurvedBattlePowerExact(entity);
        if (!Double.isFinite(battlePower) || battlePower <= 0D) {
            cir.setReturnValue(0F);
            return;
        }
        cir.setReturnValue(battlePower >= Float.MAX_VALUE ? Float.MAX_VALUE : (float) battlePower);
    }
}
