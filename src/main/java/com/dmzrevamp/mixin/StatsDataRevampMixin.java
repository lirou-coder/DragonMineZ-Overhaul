package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.DmzRevampHelper;
import com.dmzrevamp.config.LevelingRevampConfig;
import com.dmzrevamp.revamp.battlepower.CustomBattlePowerCalculator;
import com.dmzrevamp.revamp.battlepower.BattlePowerCacheControl;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StatsData.class)
public abstract class StatsDataRevampMixin implements BattlePowerCacheControl {
    @Unique private long dmzrevamp$battlePowerSignature = Long.MIN_VALUE;
    @Unique private double dmzrevamp$cachedBattlePower;
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
            dmzrevamp$cachedBattlePower = CustomBattlePowerCalculator.calculatePlayerBattlePower(data);
            dmzrevamp$battlePowerSignature = signature;
        }
        cir.setReturnValue(dmzrevamp$cachedBattlePower);
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
