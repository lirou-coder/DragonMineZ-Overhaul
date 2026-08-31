package com.dmzrevamp.mixin;

import com.dmzrevamp.compat.DmzSkillProgressionCompat;
import com.dmzrevamp.revamp.ki.KiAttackCategory;
import com.dmzrevamp.revamp.ki.KiAttackExtraEffect;
import com.dmzrevamp.config.CustomStrikeAttacksConfig;
import com.dmzrevamp.revamp.strike.CustomStrikeType;
import com.dmzrevamp.revamp.strike.RevampStrikeAttackData;
import com.dmzrevamp.revamp.strike.StrikeAttackCategoryRules;
import com.dmzrevamp.revamp.strike.StrikeAttackTemplates;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.techniques.StrikeAttackData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StrikeAttackData.class)
public abstract class StrikeAttackDataRevampExtensionMixin implements RevampStrikeAttackData {
    @Unique
    private CustomStrikeType dmzrevamp$strikeType = CustomStrikeType.BASIC;
    @Unique
    private boolean dmzrevamp$customStrike = false;
    @Unique
    private float dmzrevamp$dashSpeedMultiplier = 1.0F;
    @Unique
    private int dmzrevamp$speedLevel = 0;
    @Unique
    private int dmzrevamp$armorPenetration = 0;
    @Unique
    private int dmzrevamp$armorPenLevel = 0;
    @Unique
    private KiAttackData.SecondaryEffectType dmzrevamp$secondaryType = KiAttackData.SecondaryEffectType.NONE;
    @Unique
    private KiAttackData.AffectedStat dmzrevamp$secondaryStat = KiAttackData.AffectedStat.STR;
    @Unique
    private float dmzrevamp$secondaryIntensity = 5.0F;
    @Unique
    private int dmzrevamp$secondaryDuration = 1;
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

    @Inject(method = "save", at = @At("RETURN"), remap = false)
    private void dmzrevamp$saveExtendedStrikeData(CallbackInfoReturnable<CompoundTag> cir) {
        CompoundTag tag = cir.getReturnValue();
        CompoundTag revamp = new CompoundTag();
        revamp.putBoolean("CustomStrike", dmzrevamp$customStrike);
        revamp.putString("StrikeType", dmzrevamp$strikeType.name());
        revamp.putFloat("DashSpeedMultiplier", dmzrevamp$dashSpeedMultiplier);
        revamp.putInt("SpeedLevel", dmzrevamp$speedLevel);
        revamp.putInt("ArmorPenetration", dmzrevamp$armorPenetration);
        revamp.putInt("ArmorPenLevel", dmzrevamp$armorPenLevel);
        revamp.putString("SecondaryType", dmzrevamp$secondaryType.name());
        revamp.putString("SecondaryStat", dmzrevamp$secondaryStat.name());
        revamp.putFloat("SecondaryIntensity", dmzrevamp$secondaryIntensity);
        revamp.putInt("SecondaryDuration", dmzrevamp$secondaryDuration);
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
        tag.put("DmzRevampStrike", revamp);
    }

    @Inject(method = "load", at = @At("RETURN"), remap = false)
    private void dmzrevamp$loadExtendedStrikeData(CompoundTag tag, CallbackInfo ci) {
        if (!tag.contains("DmzRevampStrike", 10)) {
            return;
        }
        CompoundTag revamp = tag.getCompound("DmzRevampStrike");
        dmzrevamp$customStrike = revamp.contains("CustomStrike") ? revamp.getBoolean("CustomStrike") : true;
        dmzrevamp$strikeType = CustomStrikeType.parse(revamp.getString("StrikeType"));
        dmzrevamp$dashSpeedMultiplier = Mth.clamp(revamp.getFloat("DashSpeedMultiplier"), 0.0F, 1.5F);
        dmzrevamp$speedLevel = Mth.clamp(revamp.getInt("SpeedLevel"), 0, 100);
        dmzrevamp$armorPenetration = dmzrevamp$strikeType.isEvasive() ? 0 : Mth.clamp(revamp.getInt("ArmorPenetration"), 0, 10);
        dmzrevamp$armorPenLevel = Mth.clamp(revamp.getInt("ArmorPenLevel"), 0, 100);
        try {
            dmzrevamp$secondaryType = KiAttackData.SecondaryEffectType.valueOf(revamp.getString("SecondaryType"));
        } catch (IllegalArgumentException ignored) {
            dmzrevamp$secondaryType = KiAttackData.SecondaryEffectType.NONE;
        }
        try {
            dmzrevamp$secondaryStat = KiAttackData.AffectedStat.valueOf(revamp.getString("SecondaryStat"));
        } catch (IllegalArgumentException ignored) {
            dmzrevamp$secondaryStat = KiAttackData.AffectedStat.STR;
        }
        dmzrevamp$secondaryIntensity = Mth.clamp(revamp.getFloat("SecondaryIntensity"), 5.0F, 50.0F);
        dmzrevamp$secondaryDuration = Mth.clamp(revamp.getInt("SecondaryDuration"), 1, 8);
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
        dmzrevamp$thirdIntensity = Mth.clamp(revamp.getFloat("ThirdIntensity"), 5.0F, 50.0F);
        dmzrevamp$thirdDuration = Mth.clamp(revamp.getInt("ThirdDuration"), 1, 8);
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
        dmzrevamp$fourthIntensity = Mth.clamp(revamp.getFloat("FourthIntensity"), 5.0F, 50.0F);
        dmzrevamp$fourthDuration = Mth.clamp(revamp.getInt("FourthDuration"), 1, 8);
        dmzrevamp$extraOne.load(revamp.getCompound("ExtraEffect1"));
        dmzrevamp$extraTwo.load(revamp.getCompound("ExtraEffect2"));
        if (dmzrevamp$customStrike) {
            DmzSkillProgressionCompat.registerCustomStrike(((StrikeAttackData) (Object) this).getId());
        }
    }

    @Inject(method = "getCalculatedCost", at = @At("RETURN"), cancellable = true, remap = false)
    private void dmzrevamp$includeCustomStrikeCost(StatsData data, CallbackInfoReturnable<Double> cir) {
        if (!dmzrevamp$customStrike) {
            cir.setReturnValue(cir.getReturnValueD() * dmzrevamp$getExtraCostMultiplier()
                    * com.dmzrevamp.revamp.classes.skills.CustomClassPassiveEvents.strikeCostMultiplier(data));
            return;
        }
        StrikeAttackData self = (StrikeAttackData) (Object) this;
        CustomStrikeAttacksConfig.StrikeSettings settings = CustomStrikeAttacksConfig.resolve(self);
        if (StrikeAttackTemplates.SLEEP_RECOVERY.equals(self.getId())) {
            double kiCostMultiplier = settings.kiCostMultiplier;
            double maxKiBasedPower = data.getMaxEnergy() * self.getActualDamageMultiplier();
            cir.setReturnValue(Math.max(5.0D, maxKiBasedPower * 0.35D * kiCostMultiplier / 2.0D)
                    * com.dmzrevamp.revamp.classes.skills.CustomClassPassiveEvents.strikeCostMultiplier(data));
            return;
        }
        if (StrikeAttackTemplates.NAMEKIAN_REGENERATION.equals(self.getId())) {
            double kiCostMultiplier = settings.kiCostMultiplier;
            double defenseBasedPower = Math.max(0.0D, data.getDefense()) * self.getActualDamageMultiplier();
            cir.setReturnValue(Math.max(5.0D, defenseBasedPower * 0.35D * kiCostMultiplier / 2.0D)
                    * com.dmzrevamp.revamp.classes.skills.CustomClassPassiveEvents.strikeCostMultiplier(data));
            return;
        }
        double damage = data.getMeleeDamageNoMultipliers() * self.getActualDamageMultiplier();
        double kiCostMultiplier = settings.kiCostMultiplier;
        cir.setReturnValue(Math.max(5.0D, damage * 0.35D * kiCostMultiplier / 2.0D)
                * dmzrevamp$getExtraCostMultiplier()
                * com.dmzrevamp.revamp.classes.skills.CustomClassPassiveEvents.strikeCostMultiplier(data));
    }

    @Inject(method = "getActualCooldown", at = @At("RETURN"), cancellable = true, remap = false)
    private void dmzrevamp$includeCustomStrikeCooldown(CallbackInfoReturnable<Integer> cir) {
        if (dmzrevamp$customStrike) {
            StrikeAttackData self = (StrikeAttackData) (Object) this;
            int baseCooldown = CustomStrikeAttacksConfig.resolve(self).cooldownTicks;
            int cooldown = Math.max(1, Math.round(baseCooldown * self.getReductionLevelMultiplier(self.getCooldownLevel())));
            cir.setReturnValue(cooldown);
            return;
        }
        int cooldown = Math.max(1, Math.round((cir.getReturnValueI() + dmzrevamp$getExtraCooldownTicks()) * dmzrevamp$strikeType.cooldownMultiplier()));
        cir.setReturnValue(cooldown);
    }

    @Inject(method = "getActualDamageMultiplier", at = @At("RETURN"), cancellable = true, remap = false)
    private void dmzrevamp$applyCustomStrikeDamageConfig(CallbackInfoReturnable<Float> cir) {
        if (!dmzrevamp$customStrike) {
            return;
        }
        StrikeAttackData self = (StrikeAttackData) (Object) this;
        cir.setReturnValue((float) (cir.getReturnValueF() * CustomStrikeAttacksConfig.resolve(self).damageMultiplier));
    }

    @Inject(method = "getActualCastTime", at = @At("RETURN"), cancellable = true, remap = false)
    private void dmzrevamp$applyCustomStrikeCastTimeConfig(CallbackInfoReturnable<Integer> cir) {
        if (dmzrevamp$customStrike) {
            cir.setReturnValue(CustomStrikeAttacksConfig.resolve((StrikeAttackData) (Object) this).castTimeTicks);
        }
    }

    @Inject(method = "getXpGainPerHit", at = @At("RETURN"), cancellable = true, remap = false)
    private void dmzrevamp$applyCustomStrikeHitXpConfig(CallbackInfoReturnable<Integer> cir) {
        if (dmzrevamp$customStrike) {
            CustomStrikeAttacksConfig.StrikeSettings settings = CustomStrikeAttacksConfig.resolve((StrikeAttackData) (Object) this);
            cir.setReturnValue(Math.max(0, (int) Math.round(settings.xpGainPerHit * settings.xpGainMultiplier)));
        }
    }

    @Inject(method = "getXpGainPerKill", at = @At("RETURN"), cancellable = true, remap = false)
    private void dmzrevamp$applyCustomStrikeKillXpConfig(CallbackInfoReturnable<Integer> cir) {
        if (dmzrevamp$customStrike) {
            CustomStrikeAttacksConfig.StrikeSettings settings = CustomStrikeAttacksConfig.resolve((StrikeAttackData) (Object) this);
            cir.setReturnValue(Math.max(0, (int) Math.round(settings.xpGainPerKill * settings.xpGainMultiplier)));
        }
    }

    @Inject(method = "canUpgradeStat", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzrevamp$allowCustomStrikeUpgradeStats(String stat, CallbackInfoReturnable<Boolean> cir) {
        if (!dmzrevamp$customStrike) {
            return;
        }
        cir.setReturnValue(switch (stat) {
            case "damage", "cooldown" -> true;
            case "speed" -> !dmzrevamp$strikeType.isEvasive() && dmzrevamp$getDashSpeedMultiplier() < 1.5F;
            case "armor_pen" -> !dmzrevamp$strikeType.isEvasive() && dmzrevamp$getArmorPenetration() < 10;
            default -> false;
        });
    }

    @Inject(method = "getUpgradeXpCost", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzrevamp$customStrikeUpgradeXpCost(String stat, CallbackInfoReturnable<Integer> cir) {
        if (!dmzrevamp$customStrike) {
            return;
        }
        StrikeAttackData self = (StrikeAttackData) (Object) this;
        CustomStrikeAttacksConfig.StrikeSettings config = CustomStrikeAttacksConfig.resolve(self);
        int minCost = config.minXPCost;
        double multiplier = config.xpCostMultiplier;
        int totalLevels = Math.max(0, self.getDamageLevel())
                + Math.max(0, self.getCooldownLevel())
                + Math.max(0, dmzrevamp$speedLevel)
                + Math.max(0, dmzrevamp$armorPenLevel);
        int baseCost = Math.max(0, (int) Math.round(minCost * multiplier));
        int levelCost = Math.max(0, (int) Math.round(totalLevels * Math.max(0.0D, minCost * (multiplier - 1.0D))));
        int cost = Math.max(0, baseCost + levelCost);
        int maxCost = config.maxXPCost;
        if (maxCost >= 0) {
            cost = Math.min(cost, maxCost);
        }
        cir.setReturnValue(cost);
    }

    @Override
    public CustomStrikeType dmzrevamp$getStrikeType() {
        return dmzrevamp$strikeType;
    }

    @Override
    public void dmzrevamp$setStrikeType(CustomStrikeType type) {
        dmzrevamp$strikeType = type == null ? CustomStrikeType.BASIC : type;
    }

    @Override
    public boolean dmzrevamp$isCustomStrike() {
        return dmzrevamp$customStrike;
    }

    @Override
    public void dmzrevamp$setCustomStrike(boolean customStrike) {
        dmzrevamp$customStrike = customStrike;
        if (customStrike) {
            DmzSkillProgressionCompat.registerCustomStrike(((StrikeAttackData) (Object) this).getId());
        }
    }

    @Override
    public float dmzrevamp$getDashSpeedMultiplier() {
        return dmzrevamp$strikeType.isEvasive() ? 0.0F : dmzrevamp$dashSpeedMultiplier;
    }

    @Override
    public void dmzrevamp$setDashSpeedMultiplier(float speedMultiplier) {
        dmzrevamp$dashSpeedMultiplier = dmzrevamp$strikeType.isEvasive() ? 0.0F : Mth.clamp(speedMultiplier, 0.1F, 1.5F);
    }

    @Override
    public int dmzrevamp$getSpeedLevel() {
        return dmzrevamp$speedLevel;
    }

    @Override
    public void dmzrevamp$setSpeedLevel(int level) {
        dmzrevamp$speedLevel = Mth.clamp(level, 0, 100);
    }

    @Override
    public int dmzrevamp$getArmorPenetration() {
        return dmzrevamp$strikeType.isEvasive() ? 0 : dmzrevamp$armorPenetration;
    }

    @Override
    public void dmzrevamp$setArmorPenetration(int armorPenetration) {
        dmzrevamp$armorPenetration = dmzrevamp$strikeType.isEvasive() ? 0 : Mth.clamp(armorPenetration, 0, 10);
    }

    @Override
    public int dmzrevamp$getArmorPenLevel() {
        return dmzrevamp$armorPenLevel;
    }

    @Override
    public void dmzrevamp$setArmorPenLevel(int level) {
        dmzrevamp$armorPenLevel = Mth.clamp(level, 0, 100);
    }

    @Override
    public KiAttackData.SecondaryEffectType dmzrevamp$getSecondaryEffectType() {
        return dmzrevamp$secondaryType;
    }

    @Override
    public KiAttackData.AffectedStat dmzrevamp$getSecondaryAffectedStat() {
        return dmzrevamp$secondaryStat;
    }

    @Override
    public float dmzrevamp$getSecondaryIntensity() {
        return dmzrevamp$secondaryIntensity;
    }

    @Override
    public int dmzrevamp$getSecondaryDuration() {
        return dmzrevamp$secondaryDuration;
    }

    @Override
    public void dmzrevamp$setSecondaryEffect(KiAttackData.SecondaryEffectType type, KiAttackData.AffectedStat stat, float intensity, int duration) {
        dmzrevamp$secondaryType = type == null ? KiAttackData.SecondaryEffectType.NONE : type;
        dmzrevamp$secondaryStat = stat == null ? KiAttackData.AffectedStat.STR : stat;
        dmzrevamp$secondaryIntensity = Mth.clamp(intensity, 5.0F, 50.0F);
        dmzrevamp$secondaryDuration = Mth.clamp(duration, 1, 8);
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
        dmzrevamp$thirdIntensity = Mth.clamp(intensity, 5.0F, 50.0F);
        dmzrevamp$thirdDuration = Mth.clamp(duration, 1, 8);
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
        dmzrevamp$fourthIntensity = Mth.clamp(intensity, 5.0F, 50.0F);
        dmzrevamp$fourthDuration = Mth.clamp(duration, 1, 8);
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
        return StrikeAttackCategoryRules.classify((StrikeAttackData) (Object) this);
    }

    @Override
    public float dmzrevamp$getExtraCostMultiplier() {
        return 1.0F
                + (dmzrevamp$secondaryType == KiAttackData.SecondaryEffectType.NONE ? 0.0F : StrikeAttackCategoryRules.secondaryWeight(dmzrevamp$secondaryIntensity, dmzrevamp$secondaryDuration))
                + (dmzrevamp$getArmorPenetration() <= 0 ? 0.0F : dmzrevamp$getArmorPenetration() * 0.02F)
                + (dmzrevamp$thirdType == KiAttackData.SecondaryEffectType.NONE ? 0.0F : StrikeAttackCategoryRules.secondaryWeight(dmzrevamp$thirdIntensity, dmzrevamp$thirdDuration))
                + (dmzrevamp$fourthType == KiAttackData.SecondaryEffectType.NONE ? 0.0F : StrikeAttackCategoryRules.secondaryWeight(dmzrevamp$fourthIntensity, dmzrevamp$fourthDuration))
                + dmzrevamp$extraOne.costWeight()
                + dmzrevamp$extraTwo.costWeight();
    }

    @Override
    public int dmzrevamp$getExtraCooldownTicks() {
        return Math.round((dmzrevamp$getExtraCostMultiplier() - 1.0F) * 80.0F);
    }

}
