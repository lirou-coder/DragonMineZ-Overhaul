package com.dmzrevamp.revamp.quest;

import java.util.List;

/** Per-objective storage shared by KILL and SURVIVE_WAVES objectives. */
public final class RevampObjectiveDataState {
    Double defense;
    Double armor;
    Double armorToughness;
    Double protection;
    Double movementSpeed;
    Double transformArmor;
    Double transformDefense;
    Double transformArmorToughness;
    Double transformProtection;
    Double transformMovementSpeed;
    Double transformArmorMultiplier;
    Double transformDefenseMultiplier;
    Double transformArmorToughnessMultiplier;
    Double transformProtectionMultiplier;
    Double transformMovementSpeedMultiplier;
    List<QuestMobEffectConfig> mobEffects = List.of();
    List<QuestMobEffectConfig> transformMobEffects = List.of();
    final TransformStageOverrides[] transformStages = {
            TransformStageOverrides.EMPTY, TransformStageOverrides.EMPTY, TransformStageOverrides.EMPTY
    };
    final boolean[] canTransformStages = {true, true, true};
    final String[] transformEntities = new String[4];
}
