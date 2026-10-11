package com.dmzrevamp.revamp.strike;

import net.minecraft.world.entity.LivingEntity;

/** Small bridge implemented by DMZ saga NPCs and Living World's copied saga combat entity. */
public interface NpcStrikeDelayAccess {
    boolean dmzrevamp$isStrikeDelayStunned();
    boolean dmzrevamp$wasKiChargingBeforeStrikeDelay();
    void dmzrevamp$setKiChargingForStrikeDelay(boolean charging);
    void dmzrevamp$setComboingForStrikeDelay(boolean comboing);
    LivingEntity dmzrevamp$getStrikeDelayTarget();
}
