package com.dmzrevamp.revamp.strike;

import com.dmzrevamp.revamp.ki.KiAttackCategory;
import com.dmzrevamp.revamp.ki.KiAttackExtraEffect;
import com.dragonminez.common.stats.techniques.KiAttackData;

public interface RevampStrikeAttackData {
    CustomStrikeType dmzrevamp$getStrikeType();

    void dmzrevamp$setStrikeType(CustomStrikeType type);

    boolean dmzrevamp$isCustomStrike();

    void dmzrevamp$setCustomStrike(boolean customStrike);

    float dmzrevamp$getDashSpeedMultiplier();

    void dmzrevamp$setDashSpeedMultiplier(float speedMultiplier);

    int dmzrevamp$getSpeedLevel();

    void dmzrevamp$setSpeedLevel(int level);

    int dmzrevamp$getArmorPenetration();

    void dmzrevamp$setArmorPenetration(int armorPenetration);

    int dmzrevamp$getArmorPenLevel();

    void dmzrevamp$setArmorPenLevel(int level);

    KiAttackData.SecondaryEffectType dmzrevamp$getSecondaryEffectType();

    KiAttackData.AffectedStat dmzrevamp$getSecondaryAffectedStat();

    float dmzrevamp$getSecondaryIntensity();

    int dmzrevamp$getSecondaryDuration();

    void dmzrevamp$setSecondaryEffect(KiAttackData.SecondaryEffectType type, KiAttackData.AffectedStat stat, float intensity, int duration);

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

    KiAttackExtraEffect dmzrevamp$getExtraEffectOne();

    KiAttackExtraEffect dmzrevamp$getExtraEffectTwo();

    KiAttackCategory dmzrevamp$getCategory();

    float dmzrevamp$getExtraCostMultiplier();

    int dmzrevamp$getExtraCooldownTicks();
}
