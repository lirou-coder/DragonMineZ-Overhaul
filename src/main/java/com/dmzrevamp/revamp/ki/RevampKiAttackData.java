package com.dmzrevamp.revamp.ki;

import com.dragonminez.common.stats.techniques.KiAttackData;

public interface RevampKiAttackData {
    KiAttackData.SecondaryEffectType dmzrevamp$getThirdEffectType();

    KiAttackData.AffectedStat dmzrevamp$getThirdAffectedStat();

    float dmzrevamp$getThirdIntensity();

    int dmzrevamp$getThirdDuration();

    void dmzrevamp$setThirdEffect(KiAttackData.SecondaryEffectType type, KiAttackData.AffectedStat stat, float intensity, int duration);

    KiAttackData.SecondaryEffectType dmzrevamp$getFourthEffectType();

    KiAttackData.AffectedStat dmzrevamp$getFourthAffectedStat();

    float dmzrevamp$getFourthIntensity();

    int dmzrevamp$getFourthDuration();

    void dmzrevamp$setFourthEffect(KiAttackData.SecondaryEffectType type, KiAttackData.AffectedStat stat, float intensity, int duration);

    KiAttackArchetype dmzrevamp$getArchetype();

    int dmzrevamp$getMultiCastCount();

    int dmzrevamp$getDomainDurationSeconds();

    void dmzrevamp$setArchetype(KiAttackArchetype archetype, int multiCastCount, int domainDurationSeconds);

    boolean dmzrevamp$isAreaBothUtility();

    void dmzrevamp$setAreaBothUtility(boolean areaBothUtility);

    boolean dmzrevamp$isContinuous();

    void dmzrevamp$setContinuous(boolean continuous);

    KiAttackExtraEffect dmzrevamp$getExtraEffectOne();

    KiAttackExtraEffect dmzrevamp$getExtraEffectTwo();

    KiAttackCategory dmzrevamp$getCategory();

    float dmzrevamp$getExtraCostMultiplier();

    int dmzrevamp$getExtraCooldownTicks();
}
