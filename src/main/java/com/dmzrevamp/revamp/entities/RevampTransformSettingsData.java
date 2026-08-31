package com.dmzrevamp.revamp.entities;

import com.dmzrevamp.revamp.quest.QuestMobEffectConfig;

import java.util.List;

public interface RevampTransformSettingsData {
    Double dmzrevamp$getTransformArmor();

    void dmzrevamp$setTransformArmor(Double value);

    Double dmzrevamp$getTransformArmorToughness();

    void dmzrevamp$setTransformArmorToughness(Double value);

    Double dmzrevamp$getTransformProtection();

    void dmzrevamp$setTransformProtection(Double value);

    Double dmzrevamp$getTransformMovementSpeed();

    void dmzrevamp$setTransformMovementSpeed(Double value);

    Double dmzrevamp$getTransformArmorMultiplier();

    void dmzrevamp$setTransformArmorMultiplier(Double value);

    Double dmzrevamp$getTransformArmorToughnessMultiplier();

    void dmzrevamp$setTransformArmorToughnessMultiplier(Double value);

    Double dmzrevamp$getTransformProtectionMultiplier();

    void dmzrevamp$setTransformProtectionMultiplier(Double value);

    Double dmzrevamp$getTransformMovementSpeedMultiplier();

    void dmzrevamp$setTransformMovementSpeedMultiplier(Double value);

    List<QuestMobEffectConfig> dmzrevamp$getTransformMobEffects();

    void dmzrevamp$setTransformMobEffects(List<QuestMobEffectConfig> effects);
}
