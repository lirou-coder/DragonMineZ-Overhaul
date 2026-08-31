package com.dmzrevamp.revamp.quest;

import java.util.List;

public interface RevampKillObjectiveData {
    boolean dmzrevamp$canTransformStage(int stage);

    void dmzrevamp$setCanTransformStage(int stage, boolean allowed);

    TransformStageOverrides dmzrevamp$getTransformStage(int stage);

    void dmzrevamp$setTransformStage(int stage, TransformStageOverrides values);

    Double dmzrevamp$getArmor();

    void dmzrevamp$setArmor(Double value);

    Double dmzrevamp$getArmorToughness();

    void dmzrevamp$setArmorToughness(Double value);

    Double dmzrevamp$getProtection();

    void dmzrevamp$setProtection(Double value);

    Double dmzrevamp$getMovementSpeed();

    void dmzrevamp$setMovementSpeed(Double value);

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

    List<QuestMobEffectConfig> dmzrevamp$getMobEffects();

    void dmzrevamp$setMobEffects(List<QuestMobEffectConfig> mobEffects);

    List<QuestMobEffectConfig> dmzrevamp$getTransformMobEffects();

    void dmzrevamp$setTransformMobEffects(List<QuestMobEffectConfig> mobEffects);
}
