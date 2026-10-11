package com.dmzrevamp.mixin.compat;

import com.dmzrevamp.revamp.strike.NpcStrikeDelayAccess;
import com.dmzrevamp.revamp.strike.StrikeAttackDelayManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Coerce;

@Mixin(targets = "com.dmzlivingworld.entity.combat.LivingWorldSagasEntity", remap = false)
public abstract class LivingWorldSagasEntityStrikeDelayMixin implements NpcStrikeDelayAccess {
    @Shadow public abstract boolean isStunned();
    @Shadow public abstract boolean isCharge();
    @Shadow public abstract void setKiCharge(boolean charging);
    @Shadow public abstract void setComboing(boolean comboing);

    @Redirect(method = "startCombo", at = @At(value = "INVOKE",
            target = "Lcom/dmzlivingworld/entity/combat/LivingWorldSagasEntity;setComboing(Z)V"), require = 1)
    private void dmzrevamp$delayLivingWorldCombo(@Coerce Object entity, boolean comboing) {
        if (comboing) StrikeAttackDelayManager.beginNpcCombo((LivingEntity) entity);
        else setComboing(false);
    }

    @Override public boolean dmzrevamp$isStrikeDelayStunned() { return isStunned(); }
    @Override public boolean dmzrevamp$wasKiChargingBeforeStrikeDelay() { return isCharge(); }
    @Override public void dmzrevamp$setKiChargingForStrikeDelay(boolean charging) { setKiCharge(charging); }
    @Override public void dmzrevamp$setComboingForStrikeDelay(boolean comboing) { setComboing(comboing); }
    @Override public LivingEntity dmzrevamp$getStrikeDelayTarget() { return ((Mob) (Object) this).getTarget(); }
}
