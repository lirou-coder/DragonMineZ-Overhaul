package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.quest.RevampDefenseObjectiveData;
import com.dragonminez.common.quest.objectives.EscortObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EscortObjective.class)
public abstract class EscortObjectiveDefenseMixin implements RevampDefenseObjectiveData {
    @Unique private Double dmzrevamp$defense;

    @Override public Double dmzrevamp$getDefense() { return dmzrevamp$defense; }
    @Override public void dmzrevamp$setDefense(Double value) { dmzrevamp$defense = value; }
}
