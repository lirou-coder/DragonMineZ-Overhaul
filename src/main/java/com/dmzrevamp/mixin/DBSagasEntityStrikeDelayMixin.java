package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.strike.StrikeAttackDelayManager;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = DBSagasEntity.class, remap = false)
public abstract class DBSagasEntityStrikeDelayMixin {
    @Redirect(
            method = "startCombo",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dragonminez/common/init/entities/sagas/DBSagasEntity;setComboing(Z)V"
            ),
            require = 1
    )
    private void dmzrevamp$delayValidatedCombo(DBSagasEntity npc, boolean comboing) {
        if (comboing) StrikeAttackDelayManager.beginNpcCombo(npc);
        else npc.setComboing(false);
    }
}
