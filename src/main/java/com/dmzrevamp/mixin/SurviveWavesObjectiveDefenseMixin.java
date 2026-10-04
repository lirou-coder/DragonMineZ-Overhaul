package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.quest.RevampDefenseObjectiveData;
import com.dragonminez.common.quest.objectives.SurviveWavesObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(SurviveWavesObjective.class)
public abstract class SurviveWavesObjectiveDefenseMixin implements RevampDefenseObjectiveData {
    @Unique private Double dmzrevamp$defense;

    @Override public Double dmzrevamp$getDefense() { return dmzrevamp$defense; }
    @Override public void dmzrevamp$setDefense(Double value) { dmzrevamp$defense = value; }
}
