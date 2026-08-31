package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.quest.QuestMobEffectConfig;
import com.dmzrevamp.revamp.quest.RevampKillObjectiveData;
import com.dmzrevamp.revamp.quest.TransformStageOverrides;
import com.dragonminez.common.quest.objectives.KillObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

@Mixin(KillObjective.class)
public abstract class KillObjectiveRevampExtensionMixin implements RevampKillObjectiveData {
    @Unique
    private Double dmzrevamp$armor;
    @Unique
    private Double dmzrevamp$armorToughness;
    @Unique
    private Double dmzrevamp$protection;
    @Unique
    private Double dmzrevamp$movementSpeed;
    @Unique
    private Double dmzrevamp$transformArmor;
    @Unique
    private Double dmzrevamp$transformArmorToughness;
    @Unique
    private Double dmzrevamp$transformProtection;
    @Unique
    private Double dmzrevamp$transformMovementSpeed;
    @Unique
    private Double dmzrevamp$transformArmorMultiplier;
    @Unique
    private Double dmzrevamp$transformArmorToughnessMultiplier;
    @Unique
    private Double dmzrevamp$transformProtectionMultiplier;
    @Unique
    private Double dmzrevamp$transformMovementSpeedMultiplier;
    @Unique
    private List<QuestMobEffectConfig> dmzrevamp$mobEffects = List.of();
    @Unique
    private List<QuestMobEffectConfig> dmzrevamp$transformMobEffects = List.of();
    @Unique
    private TransformStageOverrides dmzrevamp$transformStage2 = TransformStageOverrides.EMPTY;
    @Unique
    private TransformStageOverrides dmzrevamp$transformStage3 = TransformStageOverrides.EMPTY;
    @Unique
    private boolean dmzrevamp$canTransform2 = true;
    @Unique
    private boolean dmzrevamp$canTransform3 = true;

    @Override
    public boolean dmzrevamp$canTransformStage(int stage) {
        return stage != 2 ? stage != 3 || dmzrevamp$canTransform3 : dmzrevamp$canTransform2;
    }

    @Override
    public void dmzrevamp$setCanTransformStage(int stage, boolean allowed) {
        if (stage == 2) dmzrevamp$canTransform2 = allowed;
        if (stage == 3) dmzrevamp$canTransform3 = allowed;
    }

    @Override
    public TransformStageOverrides dmzrevamp$getTransformStage(int stage) {
        return stage == 2 ? dmzrevamp$transformStage2 : stage == 3 ? dmzrevamp$transformStage3 : TransformStageOverrides.EMPTY;
    }

    @Override
    public void dmzrevamp$setTransformStage(int stage, TransformStageOverrides values) {
        TransformStageOverrides safe = values == null ? TransformStageOverrides.EMPTY : values;
        if (stage == 2) dmzrevamp$transformStage2 = safe;
        if (stage == 3) dmzrevamp$transformStage3 = safe;
    }

    @Override
    public Double dmzrevamp$getArmor() {
        return dmzrevamp$armor;
    }

    @Override
    public void dmzrevamp$setArmor(Double value) {
        dmzrevamp$armor = value;
    }

    @Override
    public Double dmzrevamp$getArmorToughness() {
        return dmzrevamp$armorToughness;
    }

    @Override
    public void dmzrevamp$setArmorToughness(Double value) {
        dmzrevamp$armorToughness = value;
    }

    @Override
    public Double dmzrevamp$getProtection() {
        return dmzrevamp$protection;
    }

    @Override
    public void dmzrevamp$setProtection(Double value) {
        dmzrevamp$protection = value;
    }

    @Override
    public Double dmzrevamp$getMovementSpeed() {
        return dmzrevamp$movementSpeed;
    }

    @Override
    public void dmzrevamp$setMovementSpeed(Double value) {
        dmzrevamp$movementSpeed = value;
    }

    @Override
    public Double dmzrevamp$getTransformArmor() {
        return dmzrevamp$transformArmor;
    }

    @Override
    public void dmzrevamp$setTransformArmor(Double value) {
        dmzrevamp$transformArmor = value;
    }

    @Override
    public Double dmzrevamp$getTransformArmorToughness() {
        return dmzrevamp$transformArmorToughness;
    }

    @Override
    public void dmzrevamp$setTransformArmorToughness(Double value) {
        dmzrevamp$transformArmorToughness = value;
    }

    @Override
    public Double dmzrevamp$getTransformProtection() {
        return dmzrevamp$transformProtection;
    }

    @Override
    public void dmzrevamp$setTransformProtection(Double value) {
        dmzrevamp$transformProtection = value;
    }

    @Override
    public Double dmzrevamp$getTransformMovementSpeed() {
        return dmzrevamp$transformMovementSpeed;
    }

    @Override
    public void dmzrevamp$setTransformMovementSpeed(Double value) {
        dmzrevamp$transformMovementSpeed = value;
    }

    @Override
    public Double dmzrevamp$getTransformArmorMultiplier() {
        return dmzrevamp$transformArmorMultiplier;
    }

    @Override
    public void dmzrevamp$setTransformArmorMultiplier(Double value) {
        dmzrevamp$transformArmorMultiplier = value;
    }

    @Override
    public Double dmzrevamp$getTransformArmorToughnessMultiplier() {
        return dmzrevamp$transformArmorToughnessMultiplier;
    }

    @Override
    public void dmzrevamp$setTransformArmorToughnessMultiplier(Double value) {
        dmzrevamp$transformArmorToughnessMultiplier = value;
    }

    @Override
    public Double dmzrevamp$getTransformProtectionMultiplier() {
        return dmzrevamp$transformProtectionMultiplier;
    }

    @Override
    public void dmzrevamp$setTransformProtectionMultiplier(Double value) {
        dmzrevamp$transformProtectionMultiplier = value;
    }

    @Override
    public Double dmzrevamp$getTransformMovementSpeedMultiplier() {
        return dmzrevamp$transformMovementSpeedMultiplier;
    }

    @Override
    public void dmzrevamp$setTransformMovementSpeedMultiplier(Double value) {
        dmzrevamp$transformMovementSpeedMultiplier = value;
    }

    @Override
    public List<QuestMobEffectConfig> dmzrevamp$getMobEffects() {
        return dmzrevamp$mobEffects;
    }

    @Override
    public void dmzrevamp$setMobEffects(List<QuestMobEffectConfig> mobEffects) {
        dmzrevamp$mobEffects = mobEffects == null ? List.of() : List.copyOf(mobEffects);
    }

    @Override
    public List<QuestMobEffectConfig> dmzrevamp$getTransformMobEffects() {
        return dmzrevamp$transformMobEffects;
    }

    @Override
    public void dmzrevamp$setTransformMobEffects(List<QuestMobEffectConfig> mobEffects) {
        dmzrevamp$transformMobEffects = mobEffects == null ? List.of() : List.copyOf(mobEffects);
    }
}
