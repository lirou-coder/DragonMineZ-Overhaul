package com.dmzrevamp.mixin.client;

import com.dmzrevamp.revamp.battlepower.ManualBattlePowerStatEvents;
import com.dragonminez.client.systems.kisense.KiSenseScan;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = KiSenseScan.class, remap = false)
public abstract class KiSenseScanBattlePowerMixin {
    @Inject(method = "getEntityBP", at = @At("HEAD"), cancellable = true, require = 0)
    private static void dmzrevamp$useLongManualBattlePower(LivingEntity entity, CallbackInfoReturnable<Float> cir) {
        if (entity instanceof Player) {
            return;
        }

        if (ManualBattlePowerStatEvents.isKiSenseHiddenEntity(entity)) {
            cir.setReturnValue(Float.MAX_VALUE);
            return;
        }

        long battlePower = ManualBattlePowerStatEvents.displayedBattlePower(entity, -1L);
        if (battlePower >= 0L) {
            cir.setReturnValue((float) battlePower);
        }
    }
}
