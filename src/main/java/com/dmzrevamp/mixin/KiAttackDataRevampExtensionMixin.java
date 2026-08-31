package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.ki.KiAttackArchetype;
import com.dmzrevamp.revamp.ki.KiAttackCategory;
import com.dmzrevamp.revamp.ki.KiAttackCategoryRules;
import com.dmzrevamp.revamp.ki.KiAttackExtraEffect;
import com.dmzrevamp.revamp.ki.RevampKiAttackData;
import com.dragonminez.common.stats.techniques.KiAttackData;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KiAttackData.class)
public abstract class KiAttackDataRevampExtensionMixin implements RevampKiAttackData {
    @Unique
    private KiAttackData.SecondaryEffectType dmzrevamp$thirdType = KiAttackData.SecondaryEffectType.NONE;
    @Unique
    private KiAttackData.AffectedStat dmzrevamp$thirdStat = KiAttackData.AffectedStat.STR;
    @Unique
    private float dmzrevamp$thirdIntensity = 5.0F;
    @Unique
    private int dmzrevamp$thirdDuration = 1;
    @Unique
    private KiAttackData.SecondaryEffectType dmzrevamp$fourthType = KiAttackData.SecondaryEffectType.NONE;
    @Unique
    private KiAttackData.AffectedStat dmzrevamp$fourthStat = KiAttackData.AffectedStat.STR;
    @Unique
    private float dmzrevamp$fourthIntensity = 5.0F;
    @Unique
    private int dmzrevamp$fourthDuration = 1;
    @Unique
    private final KiAttackExtraEffect dmzrevamp$extraOne = new KiAttackExtraEffect();
    @Unique
    private final KiAttackExtraEffect dmzrevamp$extraTwo = new KiAttackExtraEffect();
    @Unique
    private KiAttackArchetype dmzrevamp$archetype = KiAttackArchetype.NORMAL;
    @Unique
    private int dmzrevamp$multiCastCount = 1;
    @Unique
    private int dmzrevamp$domainDurationSeconds = 30;
    @Unique
    private boolean dmzrevamp$areaBothUtility = false;
    @Unique
    private boolean dmzrevamp$continuous = false;

    @Inject(method = "save", at = @At("RETURN"), remap = false)
    private void dmzrevamp$saveExtendedKiData(CallbackInfoReturnable<CompoundTag> cir) {
        CompoundTag tag = cir.getReturnValue();
        CompoundTag revamp = new CompoundTag();
        revamp.putString("ThirdType", dmzrevamp$thirdType.name());
        revamp.putString("ThirdStat", dmzrevamp$thirdStat.name());
        revamp.putFloat("ThirdIntensity", dmzrevamp$thirdIntensity);
        revamp.putInt("ThirdDuration", dmzrevamp$thirdDuration);
        revamp.putString("FourthType", dmzrevamp$fourthType.name());
        revamp.putString("FourthStat", dmzrevamp$fourthStat.name());
        revamp.putFloat("FourthIntensity", dmzrevamp$fourthIntensity);
        revamp.putInt("FourthDuration", dmzrevamp$fourthDuration);
        revamp.put("ExtraEffect1", dmzrevamp$extraOne.save());
        revamp.put("ExtraEffect2", dmzrevamp$extraTwo.save());
        revamp.putString("Archetype", dmzrevamp$archetype.name());
        revamp.putInt("MultiCastCount", dmzrevamp$multiCastCount);
        revamp.putInt("DomainDuration", dmzrevamp$domainDurationSeconds);
        revamp.putBoolean("AreaBothUtility", dmzrevamp$areaBothUtility);
        revamp.putBoolean("Continuous", false);
        tag.put("DmzRevampKi", revamp);
    }

    @Inject(method = "load", at = @At("RETURN"), remap = false)
    private void dmzrevamp$loadExtendedKiData(CompoundTag tag, CallbackInfo ci) {
        if (!tag.contains("DmzRevampKi", 10)) {
            return;
        }
        CompoundTag revamp = tag.getCompound("DmzRevampKi");
        try {
            dmzrevamp$thirdType = KiAttackData.SecondaryEffectType.valueOf(revamp.getString("ThirdType"));
        } catch (IllegalArgumentException ignored) {
            dmzrevamp$thirdType = KiAttackData.SecondaryEffectType.NONE;
        }
        try {
            dmzrevamp$thirdStat = KiAttackData.AffectedStat.valueOf(revamp.getString("ThirdStat"));
        } catch (IllegalArgumentException ignored) {
            dmzrevamp$thirdStat = KiAttackData.AffectedStat.STR;
        }
        dmzrevamp$thirdIntensity = Math.max(5.0F, Math.min(50.0F, revamp.getFloat("ThirdIntensity")));
        dmzrevamp$thirdDuration = Math.max(1, Math.min(8, revamp.getInt("ThirdDuration")));
        try {
            dmzrevamp$fourthType = KiAttackData.SecondaryEffectType.valueOf(revamp.getString("FourthType"));
        } catch (IllegalArgumentException ignored) {
            dmzrevamp$fourthType = KiAttackData.SecondaryEffectType.NONE;
        }
        try {
            dmzrevamp$fourthStat = KiAttackData.AffectedStat.valueOf(revamp.getString("FourthStat"));
        } catch (IllegalArgumentException ignored) {
            dmzrevamp$fourthStat = KiAttackData.AffectedStat.STR;
        }
        dmzrevamp$fourthIntensity = Math.max(5.0F, Math.min(50.0F, revamp.getFloat("FourthIntensity")));
        dmzrevamp$fourthDuration = Math.max(1, Math.min(8, revamp.getInt("FourthDuration")));
        dmzrevamp$extraOne.load(revamp.getCompound("ExtraEffect1"));
        dmzrevamp$extraTwo.load(revamp.getCompound("ExtraEffect2"));
        try {
            dmzrevamp$archetype = KiAttackArchetype.valueOf(revamp.getString("Archetype"));
        } catch (IllegalArgumentException ignored) {
            dmzrevamp$archetype = KiAttackArchetype.NORMAL;
        }
        if (dmzrevamp$archetype == KiAttackArchetype.MULTI_CAST) {
            dmzrevamp$archetype = KiAttackArchetype.NORMAL;
        }
        dmzrevamp$multiCastCount = Math.max(1, Math.min(dmzrevamp$maxProjectileCount(), revamp.getInt("MultiCastCount")));
        dmzrevamp$domainDurationSeconds = Math.max(30, Math.min(180, revamp.getInt("DomainDuration")));
        dmzrevamp$areaBothUtility = revamp.getBoolean("AreaBothUtility");
        dmzrevamp$continuous = false;
    }

    @Inject(method = "getCalculatedCost", at = @At("RETURN"), cancellable = true, remap = false)
    private void dmzrevamp$includeExtendedCost(CallbackInfoReturnable<Double> cir) {
        cir.setReturnValue(cir.getReturnValueD() * dmzrevamp$getExtraCostMultiplier());
    }

    @Inject(method = "getActualCooldown", at = @At("RETURN"), cancellable = true, remap = false)
    private void dmzrevamp$includeExtendedCooldown(CallbackInfoReturnable<Integer> cir) {
        int cooldown = cir.getReturnValueI() + dmzrevamp$getExtraCooldownTicks();
        if (dmzrevamp$areaBothUtility) {
            cooldown *= 2;
        }
        cir.setReturnValue(cooldown);
    }

    @Override
    public KiAttackData.SecondaryEffectType dmzrevamp$getThirdEffectType() {
        return dmzrevamp$thirdType;
    }

    @Override
    public KiAttackData.AffectedStat dmzrevamp$getThirdAffectedStat() {
        return dmzrevamp$thirdStat;
    }

    @Override
    public float dmzrevamp$getThirdIntensity() {
        return dmzrevamp$thirdIntensity;
    }

    @Override
    public int dmzrevamp$getThirdDuration() {
        return dmzrevamp$thirdDuration;
    }

    @Override
    public void dmzrevamp$setThirdEffect(KiAttackData.SecondaryEffectType type, KiAttackData.AffectedStat stat, float intensity, int duration) {
        dmzrevamp$thirdType = type == null ? KiAttackData.SecondaryEffectType.NONE : type;
        dmzrevamp$thirdStat = stat == null ? KiAttackData.AffectedStat.STR : stat;
        dmzrevamp$thirdIntensity = Math.max(5.0F, Math.min(50.0F, intensity));
        dmzrevamp$thirdDuration = Math.max(1, Math.min(8, duration));
    }

    @Override
    public KiAttackData.SecondaryEffectType dmzrevamp$getFourthEffectType() {
        return dmzrevamp$fourthType;
    }

    @Override
    public KiAttackData.AffectedStat dmzrevamp$getFourthAffectedStat() {
        return dmzrevamp$fourthStat;
    }

    @Override
    public float dmzrevamp$getFourthIntensity() {
        return dmzrevamp$fourthIntensity;
    }

    @Override
    public int dmzrevamp$getFourthDuration() {
        return dmzrevamp$fourthDuration;
    }

    @Override
    public void dmzrevamp$setFourthEffect(KiAttackData.SecondaryEffectType type, KiAttackData.AffectedStat stat, float intensity, int duration) {
        dmzrevamp$fourthType = type == null ? KiAttackData.SecondaryEffectType.NONE : type;
        dmzrevamp$fourthStat = stat == null ? KiAttackData.AffectedStat.STR : stat;
        dmzrevamp$fourthIntensity = Math.max(5.0F, Math.min(50.0F, intensity));
        dmzrevamp$fourthDuration = Math.max(1, Math.min(8, duration));
    }

    @Override
    public KiAttackArchetype dmzrevamp$getArchetype() {
        return dmzrevamp$archetype;
    }

    @Override
    public int dmzrevamp$getMultiCastCount() {
        return dmzrevamp$multiCastCount;
    }

    @Override
    public int dmzrevamp$getDomainDurationSeconds() {
        return dmzrevamp$domainDurationSeconds;
    }

    @Override
    public void dmzrevamp$setArchetype(KiAttackArchetype archetype, int multiCastCount, int domainDurationSeconds) {
        dmzrevamp$archetype = archetype == null ? KiAttackArchetype.NORMAL : archetype;
        if (dmzrevamp$archetype == KiAttackArchetype.MULTI_CAST) {
            dmzrevamp$archetype = KiAttackArchetype.NORMAL;
        }
        dmzrevamp$multiCastCount = Math.max(1, Math.min(dmzrevamp$maxProjectileCount(), multiCastCount));
        dmzrevamp$domainDurationSeconds = Math.max(30, Math.min(180, domainDurationSeconds));
    }

    @Override
    public boolean dmzrevamp$isAreaBothUtility() {
        return dmzrevamp$areaBothUtility;
    }

    @Override
    public void dmzrevamp$setAreaBothUtility(boolean areaBothUtility) {
        KiAttackData data = (KiAttackData) (Object) this;
        dmzrevamp$areaBothUtility = areaBothUtility && data.getKiType() == KiAttackData.KiType.AREA;
    }

    @Override
    public boolean dmzrevamp$isContinuous() {
        return false;
    }

    @Override
    public void dmzrevamp$setContinuous(boolean continuous) {
        dmzrevamp$continuous = false;
    }

    @Override
    public KiAttackExtraEffect dmzrevamp$getExtraEffectOne() {
        return dmzrevamp$extraOne;
    }

    @Override
    public KiAttackExtraEffect dmzrevamp$getExtraEffectTwo() {
        return dmzrevamp$extraTwo;
    }

    @Override
    public KiAttackCategory dmzrevamp$getCategory() {
        return KiAttackCategoryRules.classify((KiAttackData) (Object) this);
    }

    @Override
    public float dmzrevamp$getExtraCostMultiplier() {
        return dmzrevamp$getExtraCostMultiplier(true);
    }

    @Unique
    private float dmzrevamp$getExtraCostMultiplier(boolean includeAreaBoth) {
        KiAttackData data = (KiAttackData) (Object) this;
        float thirdWeight = dmzrevamp$thirdType == KiAttackData.SecondaryEffectType.NONE
                ? 0.0F
                : KiAttackCategoryRules.secondaryWeight(dmzrevamp$thirdIntensity, dmzrevamp$thirdDuration);
        float fourthWeight = dmzrevamp$fourthType == KiAttackData.SecondaryEffectType.NONE
                ? 0.0F
                : KiAttackCategoryRules.secondaryWeight(dmzrevamp$fourthIntensity, dmzrevamp$fourthDuration);
        float areaBothPrimaryWeight = dmzrevamp$areaBothUtility
                && data.getSecondaryEffectType() != KiAttackData.SecondaryEffectType.NONE
                && !data.hasValidSecondaryEffect()
                ? data.secondaryCostWeight()
                : 0.0F;
        float multiplier = 1.0F + thirdWeight + fourthWeight + dmzrevamp$extraOne.costWeight() + dmzrevamp$extraTwo.costWeight() + dmzrevamp$archetypeCostWeight();
        multiplier += areaBothPrimaryWeight;
        if (includeAreaBoth && dmzrevamp$areaBothUtility) {
            multiplier *= 2.0F;
        }
        return multiplier;
    }

    @Override
    public int dmzrevamp$getExtraCooldownTicks() {
        int cooldown = Math.round((dmzrevamp$getExtraCostMultiplier(false) - 1.0F) * 80.0F);
        return cooldown;
    }

    @Inject(method = "normalizeStatsForType", at = @At("RETURN"), cancellable = true, remap = false)
    private static void dmzrevamp$allowAreaSize(KiAttackData.KiType type, float damage, float size, float speed, int armorPen, CallbackInfoReturnable<float[]> cir) {
        if (type == KiAttackData.KiType.AREA) {
            float[] values = cir.getReturnValue();
            values[1] = Math.max(0.1F, Math.min(15.0F, size));
            cir.setReturnValue(values);
        }
    }

    @Unique
    private float dmzrevamp$archetypeCostWeight() {
        return dmzrevamp$projectileCostWeight();
    }

    @Unique
    private float dmzrevamp$projectileCostWeight() {
        KiAttackData data = (KiAttackData) (Object) this;
        if ((data.getKiType() != KiAttackData.KiType.MEDIUM_BALL && data.getKiType() != KiAttackData.KiType.SMALL_BALL) || dmzrevamp$multiCastCount <= 1 || !dmzrevamp$hasAnyStatusEffect(data)) {
            return 0.0F;
        }
        return (dmzrevamp$multiCastCount - 1) * 0.15F;
    }

    @Unique
    private int dmzrevamp$maxProjectileCount() {
        KiAttackData data = (KiAttackData) (Object) this;
        if (data.getKiType() == KiAttackData.KiType.SMALL_BALL) {
            return 10;
        }
        if (data.getKiType() == KiAttackData.KiType.MEDIUM_BALL) {
            return 5;
        }
        return 1;
    }

    @Unique
    private boolean dmzrevamp$hasAnyStatusEffect(KiAttackData data) {
        return data.hasValidSecondaryEffect()
                || dmzrevamp$thirdType != KiAttackData.SecondaryEffectType.NONE
                || dmzrevamp$fourthType != KiAttackData.SecondaryEffectType.NONE
                || dmzrevamp$extraOne.isActive()
                || dmzrevamp$extraTwo.isActive();
    }

}
