package com.dmzrevamp.revamp.strike;

/** Runtime bridge for updating DMZ's captured clash power as player stats change. */
public interface ClashParticipantPowerAccess {
    void dmzrevamp$setStatPower(double power);
}
