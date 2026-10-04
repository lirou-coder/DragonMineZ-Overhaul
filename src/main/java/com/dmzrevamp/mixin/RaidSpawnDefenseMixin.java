package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.quest.ConfiguredTransformDefenseData;
import com.dragonminez.common.config.RaidDefinition;
import com.google.gson.annotations.SerializedName;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = RaidDefinition.Spawn.class, remap = false)
public abstract class RaidSpawnDefenseMixin implements ConfiguredTransformDefenseData {
    @Unique @SerializedName(value = "Defense", alternate = {"defense"})
    private Double dmzrevamp$defense;
    @Unique @SerializedName(value = "TransformDefense", alternate = {"transformDefense"})
    private Double dmzrevamp$transformDefense;

    @Override public Double dmzrevamp$getDefense() { return dmzrevamp$defense; }
    @Override public Double dmzrevamp$getTransformDefense() { return dmzrevamp$transformDefense; }
}
