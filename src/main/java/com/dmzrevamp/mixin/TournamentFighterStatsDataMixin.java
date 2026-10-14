package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.quest.TournamentFighterStatsExtension;
import com.dragonminez.common.network.TournamentPackets;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = TournamentPackets.OpenBracketS2C.FighterStats.class, remap = false)
public abstract class TournamentFighterStatsDataMixin implements TournamentFighterStatsExtension {
    @Unique private Double dmzrevamp$defense;
    @Unique private Double dmzrevamp$movementSpeed;

    @Override public Double dmzrevamp$getDefense() { return dmzrevamp$defense; }
    @Override public void dmzrevamp$setDefense(Double value) { dmzrevamp$defense = value; }
    @Override public Double dmzrevamp$getMovementSpeed() { return dmzrevamp$movementSpeed; }
    @Override public void dmzrevamp$setMovementSpeed(Double value) { dmzrevamp$movementSpeed = value; }
}
