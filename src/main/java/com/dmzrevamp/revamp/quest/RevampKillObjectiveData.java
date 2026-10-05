package com.dmzrevamp.revamp.quest;

import java.util.List;

public interface RevampKillObjectiveData extends RevampDefenseObjectiveData {
    RevampObjectiveDataState dmzrevamp$getState();

    @Override default Double dmzrevamp$getDefense() { return dmzrevamp$getState().defense; }
    @Override default void dmzrevamp$setDefense(Double value) { dmzrevamp$getState().defense = value; }

    default boolean dmzrevamp$canTransformStage(int stage) {
        return stage >= 2 && stage <= 4 ? dmzrevamp$getState().canTransformStages[stage - 2] : true;
    }
    default void dmzrevamp$setCanTransformStage(int stage, boolean allowed) {
        if (stage >= 2 && stage <= 4) dmzrevamp$getState().canTransformStages[stage - 2] = allowed;
    }
    default TransformStageOverrides dmzrevamp$getTransformStage(int stage) {
        return stage >= 2 && stage <= 4 ? dmzrevamp$getState().transformStages[stage - 2] : TransformStageOverrides.EMPTY;
    }
    default void dmzrevamp$setTransformStage(int stage, TransformStageOverrides values) {
        if (stage >= 2 && stage <= 4) dmzrevamp$getState().transformStages[stage - 2] = values == null ? TransformStageOverrides.EMPTY : values;
    }
    default String dmzrevamp$getTransformEntity(int stage) {
        return stage >= 1 && stage <= 4 ? dmzrevamp$getState().transformEntities[stage - 1] : null;
    }
    default void dmzrevamp$setTransformEntity(int stage, String entityId) {
        if (stage >= 1 && stage <= 4) {
            dmzrevamp$getState().transformEntities[stage - 1] = entityId == null || entityId.isBlank() ? null : entityId.trim();
        }
    }

    default Double dmzrevamp$getArmor() { return dmzrevamp$getState().armor; }
    default void dmzrevamp$setArmor(Double value) { dmzrevamp$getState().armor = value; }
    default Double dmzrevamp$getArmorToughness() { return dmzrevamp$getState().armorToughness; }
    default void dmzrevamp$setArmorToughness(Double value) { dmzrevamp$getState().armorToughness = value; }
    default Double dmzrevamp$getProtection() { return dmzrevamp$getState().protection; }
    default void dmzrevamp$setProtection(Double value) { dmzrevamp$getState().protection = value; }
    default Double dmzrevamp$getMovementSpeed() { return dmzrevamp$getState().movementSpeed; }
    default void dmzrevamp$setMovementSpeed(Double value) { dmzrevamp$getState().movementSpeed = value; }
    default Double dmzrevamp$getTransformArmor() { return dmzrevamp$getState().transformArmor; }
    default void dmzrevamp$setTransformArmor(Double value) { dmzrevamp$getState().transformArmor = value; }
    default Double dmzrevamp$getTransformDefense() { return dmzrevamp$getState().transformDefense; }
    default void dmzrevamp$setTransformDefense(Double value) { dmzrevamp$getState().transformDefense = value; }
    default Double dmzrevamp$getTransformArmorToughness() { return dmzrevamp$getState().transformArmorToughness; }
    default void dmzrevamp$setTransformArmorToughness(Double value) { dmzrevamp$getState().transformArmorToughness = value; }
    default Double dmzrevamp$getTransformProtection() { return dmzrevamp$getState().transformProtection; }
    default void dmzrevamp$setTransformProtection(Double value) { dmzrevamp$getState().transformProtection = value; }
    default Double dmzrevamp$getTransformMovementSpeed() { return dmzrevamp$getState().transformMovementSpeed; }
    default void dmzrevamp$setTransformMovementSpeed(Double value) { dmzrevamp$getState().transformMovementSpeed = value; }
    default Double dmzrevamp$getTransformArmorMultiplier() { return dmzrevamp$getState().transformArmorMultiplier; }
    default void dmzrevamp$setTransformArmorMultiplier(Double value) { dmzrevamp$getState().transformArmorMultiplier = value; }
    default Double dmzrevamp$getTransformDefenseMultiplier() { return dmzrevamp$getState().transformDefenseMultiplier; }
    default void dmzrevamp$setTransformDefenseMultiplier(Double value) { dmzrevamp$getState().transformDefenseMultiplier = value; }
    default Double dmzrevamp$getTransformArmorToughnessMultiplier() { return dmzrevamp$getState().transformArmorToughnessMultiplier; }
    default void dmzrevamp$setTransformArmorToughnessMultiplier(Double value) { dmzrevamp$getState().transformArmorToughnessMultiplier = value; }
    default Double dmzrevamp$getTransformProtectionMultiplier() { return dmzrevamp$getState().transformProtectionMultiplier; }
    default void dmzrevamp$setTransformProtectionMultiplier(Double value) { dmzrevamp$getState().transformProtectionMultiplier = value; }
    default Double dmzrevamp$getTransformMovementSpeedMultiplier() { return dmzrevamp$getState().transformMovementSpeedMultiplier; }
    default void dmzrevamp$setTransformMovementSpeedMultiplier(Double value) { dmzrevamp$getState().transformMovementSpeedMultiplier = value; }
    default List<QuestMobEffectConfig> dmzrevamp$getMobEffects() { return dmzrevamp$getState().mobEffects; }
    default void dmzrevamp$setMobEffects(List<QuestMobEffectConfig> effects) {
        dmzrevamp$getState().mobEffects = effects == null ? List.of() : List.copyOf(effects);
    }
    default List<QuestMobEffectConfig> dmzrevamp$getTransformMobEffects() { return dmzrevamp$getState().transformMobEffects; }
    default void dmzrevamp$setTransformMobEffects(List<QuestMobEffectConfig> effects) {
        dmzrevamp$getState().transformMobEffects = effects == null ? List.of() : List.copyOf(effects);
    }
}
