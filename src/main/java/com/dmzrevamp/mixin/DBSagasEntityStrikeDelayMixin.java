package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.strike.StrikeAttackDelayManager;
import com.dmzrevamp.revamp.strike.NpcStrikeDelayAccess;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = DBSagasEntity.class, remap = false)
public abstract class DBSagasEntityStrikeDelayMixin implements NpcStrikeDelayAccess {
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

    @Override public boolean dmzrevamp$isStrikeDelayStunned() { return ((DBSagasEntity) (Object) this).isStunned(); }
    @Override public boolean dmzrevamp$wasKiChargingBeforeStrikeDelay() { return ((DBSagasEntity) (Object) this).isCharge(); }
    @Override public void dmzrevamp$setKiChargingForStrikeDelay(boolean charging) { ((DBSagasEntity) (Object) this).setKiCharge(charging); }
    @Override public void dmzrevamp$setComboingForStrikeDelay(boolean comboing) { ((DBSagasEntity) (Object) this).setComboing(comboing); }
    @Override public net.minecraft.world.entity.LivingEntity dmzrevamp$getStrikeDelayTarget() { return ((DBSagasEntity) (Object) this).getTarget(); }
}
