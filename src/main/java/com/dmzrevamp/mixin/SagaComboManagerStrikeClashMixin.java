package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.strike.StrikeClashManager;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.dragonminez.common.init.entities.sagas.helper.ComboManager;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ComboManager.class, remap = false)
public abstract class SagaComboManagerStrikeClashMixin {
    @Inject(method = "handleCombo", at = @At("HEAD"), cancellable = true, require = 1)
    private static void dmzrevamp$pauseComboDuringStrikeClash(
            DBSagasEntity user,
            LivingEntity target,
            int comboId,
            int timer,
            CallbackInfo ci
    ) {
        if (user != null && StrikeClashManager.isClashing(user.getUUID())) {
            ci.cancel();
        }
    }
}
