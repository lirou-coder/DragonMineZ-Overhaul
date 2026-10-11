package com.dmzrevamp.mixin;

import com.dmzrevamp.config.StrikeClashConfigured;
import com.dmzrevamp.revamp.strike.StrikeClashManager;
import com.dragonminez.common.combat.clash.StrikeClash;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = DBSagasEntity.class, remap = false)
public abstract class DBSagasEntityComboClashMixin {
    @Inject(method = "handleComboLogic", at = @At("HEAD"), cancellable = true, require = 1)
    private void dmzrevamp$tryClashDuringAnyTargetedNpcCombo(CallbackInfo ci) {
        DBSagasEntity npc = (DBSagasEntity) (Object) this;
        if (!StrikeClashConfigured.get().enabled || !StrikeClashManager.canNpcStartNativeClash(npc)) return;
        if (StrikeClash.tryStart(npc)) ci.cancel();
    }
}
