package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.quest.RevampKillObjectiveData;
import com.dmzrevamp.revamp.quest.RevampObjectiveDataState;
import com.dragonminez.common.quest.objectives.SurviveWavesObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(SurviveWavesObjective.class)
public abstract class SurviveWavesObjectiveDefenseMixin implements RevampKillObjectiveData {
    @Unique private final RevampObjectiveDataState dmzrevamp$data = new RevampObjectiveDataState();

    @Override
    public RevampObjectiveDataState dmzrevamp$getState() {
        return dmzrevamp$data;
    }
}
