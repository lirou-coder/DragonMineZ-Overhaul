package com.dmzrevamp.mixin.client;

import com.dmzrevamp.client.ClientStrikeClashState;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.dragonminez.common.init.entities.sagas.helper.DBSagasAnimationHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Lets synchronized swing pulses use a saga NPC's basic attack controller during Strike Clash. */
@Mixin(value = DBSagasAnimationHandler.class, remap = false)
public abstract class DBSagasAnimationHandlerStrikeClashMixin {
    @Redirect(method = "skillPredicate", at = @At(value = "INVOKE",
            target = "Lcom/dragonminez/common/init/entities/sagas/DBSagasEntity;isComboing()Z"))
    private static boolean dmzrevamp$hideComboAnimationDuringStrikeClash(DBSagasEntity entity) {
        return !ClientStrikeClashState.isEntityActive(entity.getId()) && entity.isComboing();
    }

    @Redirect(method = "attackPredicate", at = @At(value = "INVOKE",
            target = "Lcom/dragonminez/common/init/entities/sagas/DBSagasEntity;isComboing()Z"))
    private static boolean dmzrevamp$allowBasicAttacksDuringStrikeClash(DBSagasEntity entity) {
        return !ClientStrikeClashState.isEntityActive(entity.getId()) && entity.isComboing();
    }
}
