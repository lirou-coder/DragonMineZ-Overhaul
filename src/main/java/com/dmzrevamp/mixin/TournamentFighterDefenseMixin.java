package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.quest.ConfiguredDefenseData;
import com.dmzrevamp.revamp.quest.RevampKillObjectiveData;
import com.dmzrevamp.revamp.quest.RevampObjectiveDataState;
import com.dmzrevamp.revamp.quest.ConfiguredTournamentTransformData;
import com.dragonminez.common.config.TournamentDefinition;
import com.google.gson.annotations.SerializedName;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = TournamentDefinition.Fighter.class, remap = false)
public abstract class TournamentFighterDefenseMixin implements ConfiguredDefenseData, RevampKillObjectiveData, ConfiguredTournamentTransformData {
    @Unique private final RevampObjectiveDataState dmzrevamp$state = new RevampObjectiveDataState();
    @Unique @SerializedName(value = "Defense", alternate = {"defense"})
    private Double dmzrevamp$defense;
    @Unique @SerializedName("canTransform")
    private Boolean dmzrevamp$canTransform;

    @Override public Double dmzrevamp$getDefense() { return dmzrevamp$defense; }
    @Override public RevampObjectiveDataState dmzrevamp$getState() { return dmzrevamp$state; }
    @Override public boolean dmzrevamp$isTransformEnabled() { return Boolean.TRUE.equals(dmzrevamp$canTransform); }
}
