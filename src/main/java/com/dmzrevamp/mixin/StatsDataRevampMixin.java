package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.DmzRevampHelper;
import com.dmzrevamp.config.LevelingRevampConfig;
import com.dmzrevamp.revamp.battlepower.BattlePowerCacheControl;
import com.dmzrevamp.compat.NoeaCompat;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.skills.Skill;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

@Mixin(value = StatsData.class, priority = 2000)
public abstract class StatsDataRevampMixin implements BattlePowerCacheControl {
    @Unique private long dmzrevamp$battlePowerSignature = Long.MIN_VALUE;
    @Unique private double dmzrevamp$cachedBattlePower;
    @Unique private Map<String, Integer> dmzrevamp$preLimitSkillLevels = Map.of();
    @Unique private Map<String, Integer> dmzrevamp$serializedSkillLevels = Map.of();
    @Shadow(remap = false)
    @Final
    private Player player;

    @Inject(method = "getLevel", at = @At("HEAD"), cancellable = true, remap = false)
    // Replaces DMZ's level calculation with Overhaul's six-stat point formula.
    private void dmzrevamp$useConfiguredLevelFormula(CallbackInfoReturnable<Integer> cir) {
        if (LevelingRevampConfig.levelsEnabled()) {
            cir.setReturnValue(DmzRevampHelper.getConfiguredLevel((StatsData) (Object) this));
        }
    }

    @Inject(method = "getBattlePowerExact", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzrevamp$useConfiguredBattlePowerFormula(CallbackInfoReturnable<Double> cir) {
        StatsData data = (StatsData) (Object) this;
        long signature = dmzrevamp$battlePowerSignature(data);
        if (signature != dmzrevamp$battlePowerSignature) {
            dmzrevamp$cachedBattlePower = NoeaCompat.calculateBaseBattlePower(data);
            dmzrevamp$battlePowerSignature = signature;
        }
        cir.setReturnValue(NoeaCompat.applyDynamicBattlePower(data, dmzrevamp$cachedBattlePower));
    }

    @Unique
    private static long dmzrevamp$battlePowerSignature(StatsData data) {
        long hash = 17L;
        hash = 31L * hash + Double.doubleToLongBits(data.getMaxMeleeDamage());
        hash = 31L * hash + Double.doubleToLongBits(data.getMaxStrikeDamage());
        hash = 31L * hash + Double.doubleToLongBits(data.getMaxStamina());
        hash = 31L * hash + Double.doubleToLongBits(data.getMaxDefense());
        hash = 31L * hash + Double.doubleToLongBits(data.getMaxHealth());
        hash = 31L * hash + Double.doubleToLongBits(data.getMaxKiDamage());
        hash = 31L * hash + Double.doubleToLongBits(data.getMaxEnergy());
        hash = 31L * hash + data.getResources().getPowerRelease();
        hash = 31L * hash + (data.getStatus().isAndroidUpgraded() ? 1L : 0L);
        return hash;
    }

    @Override
    public void dmzrevamp$recalculateBattlePower() {
        dmzrevamp$battlePowerSignature = Long.MIN_VALUE;
        ((StatsData) (Object) this).getBattlePowerExact();
    }

    @Inject(method = "updateTransformationSkillLimits", at = @At("HEAD"), remap = false)
    private void dmzrevamp$captureSkillsBeforeLimitRefresh(String raceName, CallbackInfo ci) {
        dmzrevamp$preLimitSkillLevels = dmzrevamp$currentSkillLevels((StatsData) (Object) this);
    }

    @Inject(method = "updateTransformationSkillLimits", at = @At("TAIL"), remap = false)
    private void dmzrevamp$restoreSkillsAfterLimitRefresh(String raceName, CallbackInfo ci) {
        dmzrevamp$restoreLoweredSkillLevels((StatsData) (Object) this, dmzrevamp$preLimitSkillLevels);
        dmzrevamp$preLimitSkillLevels = Map.of();
    }

    @Inject(method = "load", at = @At("HEAD"), remap = false)
    private void dmzrevamp$captureSerializedSkillLevels(CompoundTag nbt, CallbackInfo ci) {
        dmzrevamp$serializedSkillLevels = dmzrevamp$readSerializedSkillLevels(nbt);
    }

    @Inject(method = "load", at = @At("TAIL"), remap = false)
    private void dmzrevamp$restoreSerializedSkillLevels(CompoundTag nbt, CallbackInfo ci) {
        dmzrevamp$restoreLoweredSkillLevels((StatsData) (Object) this, dmzrevamp$serializedSkillLevels);
        dmzrevamp$serializedSkillLevels = Map.of();
    }

    @Unique
    private static Map<String, Integer> dmzrevamp$currentSkillLevels(StatsData data) {
        Map<String, Integer> levels = new HashMap<>();
        data.getSkills().getAllSkills().forEach((name, skill) -> levels.put(name.toLowerCase(), skill.getLevel()));
        return levels;
    }

    @Unique
    private static Map<String, Integer> dmzrevamp$readSerializedSkillLevels(CompoundTag statsTag) {
        if (statsTag == null || !statsTag.contains("Skills", Tag.TAG_COMPOUND)) return Map.of();
        CompoundTag skillsTag = statsTag.getCompound("Skills");
        if (!skillsTag.contains("SkillsList", Tag.TAG_LIST)) return Map.of();

        Map<String, Integer> levels = new HashMap<>();
        ListTag list = skillsTag.getList("SkillsList", Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            CompoundTag skillTag = list.getCompound(index);
            String name = skillTag.getString("Name");
            if (!name.isBlank()) levels.put(name.toLowerCase(), Math.max(0, skillTag.getInt("Level")));
        }
        return levels;
    }

    @Unique
    private static void dmzrevamp$restoreLoweredSkillLevels(StatsData data, Map<String, Integer> preservedLevels) {
        if (preservedLevels == null || preservedLevels.isEmpty()) return;
        preservedLevels.forEach((name, preservedLevel) -> {
            Skill skill = data.getSkills().getSkill(name);
            if (skill == null) {
                data.getSkills().registerDefaultSkill(name, preservedLevel);
                data.getSkills().setSkillLevel(name, preservedLevel);
            } else if (skill.getLevel() < preservedLevel) {
                skill.setMaxLevel(Math.max(skill.getMaxLevel(), preservedLevel));
                skill.setLevel(preservedLevel);
            }
        });
    }

    @Inject(method = "getMaxStrikeDamage", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzrevamp$getMaxSpeedInsteadOfStrikeDamage(CallbackInfoReturnable<Double> cir) {
        StatsData data = (StatsData) (Object) this;
        cir.setReturnValue(DmzRevampHelper.getMaxSpeedValue(data));
    }

    @Inject(method = "getStrikeDamage", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzrevamp$getSpeedInsteadOfStrikeDamage(CallbackInfoReturnable<Double> cir) {
        StatsData data = (StatsData) (Object) this;
        cir.setReturnValue(DmzRevampHelper.getCurrentSpeedValue(data));
    }

    @Inject(method = "getStrikeDamageNoForms", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzrevamp$getSpeedNoFormsInsteadOfStrikeDamage(CallbackInfoReturnable<Double> cir) {
        StatsData data = (StatsData) (Object) this;
        cir.setReturnValue(DmzRevampHelper.getCurrentSpeedNoFormsValue(data));
    }

    @Inject(method = "getMaxKiDamage", at = @At("HEAD"), cancellable = true, remap = false)
    // Gives Ki Power a base damage value of 1 plus scaled PWR, matching the new battle-power and technique math.
    private void dmzrevamp$addBaseKiDamage(CallbackInfoReturnable<Double> cir) {
        StatsData data = (StatsData) (Object) this;
        double pwrScaling = data.getStatScaling("PWR");
        double pwrMultiplier = data.getTotalMultiplier("PWR");
        int kiPower = data.getStats().getKiPower();
        double bonusPwr = data.getBonusStats().calculateBonus("PWR", kiPower, false);
        double multipliedBonusPwr = data.getBonusStats().calculateBonus("PWR", kiPower, true);
        cir.setReturnValue(1D
                + ((kiPower + multipliedBonusPwr) * pwrScaling * pwrMultiplier)
                + (bonusPwr * pwrScaling));
    }

    @Inject(method = "getKiDamage", at = @At("HEAD"), cancellable = true, remap = false)
    // Applies power release to the new base Ki damage formula for current combat damage.
    private void dmzrevamp$addBaseCurrentKiDamage(CallbackInfoReturnable<Double> cir) {
        StatsData data = (StatsData) (Object) this;
        double pwrScaling = data.getStatScaling("PWR");
        double pwrMultiplier = data.getTotalMultiplier("PWR");
        double releaseMultiplier = data.getResources().getPowerRelease() / 100D;
        int kiPower = data.getStats().getKiPower();
        double bonusPwr = data.getBonusStats().calculateBonus("PWR", kiPower, false);
        double multipliedBonusPwr = data.getBonusStats().calculateBonus("PWR", kiPower, true);
        double baseKiDamage = ((kiPower + multipliedBonusPwr) * pwrScaling * pwrMultiplier)
                + (bonusPwr * pwrScaling);
        cir.setReturnValue(1D + (baseKiDamage * releaseMultiplier));
    }

}
