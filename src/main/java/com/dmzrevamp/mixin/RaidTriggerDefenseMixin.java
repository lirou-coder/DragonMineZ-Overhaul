package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.quest.ConfiguredDefenseData;
import com.dragonminez.common.config.RaidDefinition;
import com.google.gson.annotations.SerializedName;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = RaidDefinition.Trigger.class, remap = false)
public abstract class RaidTriggerDefenseMixin implements ConfiguredDefenseData {
    @Unique @SerializedName(value = "Defense", alternate = {"defense"})
    private Double dmzrevamp$defense;

    @Override public Double dmzrevamp$getDefense() { return dmzrevamp$defense; }
}
