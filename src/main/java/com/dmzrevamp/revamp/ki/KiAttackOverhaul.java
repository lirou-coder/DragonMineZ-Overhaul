package com.dmzrevamp.revamp.ki;

import com.dmzrevamp.config.DmzRevampConfig;
import com.dmzrevamp.compat.DmzKiOverchargeCompat;
import com.dmzrevamp.revamp.DmzRevampHelper;
import com.dmzrevamp.revamp.classes.skills.ClassSkillEvents;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Resources;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.techniques.TechniqueData;
import net.minecraft.server.level.ServerPlayer;

public final class KiAttackOverhaul {
    public static final float NORMAL_MAX_PERCENT = 100.0F;
    public static final float DMZ_OVERCHARGE_MAX_PERCENT = 175.0F;
    public static final float VISUAL_OVERLOAD_START_PERCENT = DMZ_OVERCHARGE_MAX_PERCENT;
    public static final float REVAMP_OVERCHARGE_MAX_PERCENT = 400.0F;
    private static final String OVERCHARGE_PAUSED_TAG = "dmzrevamp_overcharge_paused";

    // This class only exposes shared Ki technique rules, so it should not be instantiated.
    private KiAttackOverhaul() {
    }

    // Returns the extended charge cap used by this mod.
    public static float maxChargePercent() {
        return Math.max(DMZ_OVERCHARGE_MAX_PERCENT, DmzKiOverchargeCompat.effectiveMaxChargePercent(REVAMP_OVERCHARGE_MAX_PERCENT));
    }

    // Slows charge growth after the original 175 percent overcharge cap, then applies cast-speed bonuses to the charge step.
    public static float extendChargeStep(float targetPercent, float proposedPercent, ServerPlayer player, StatsData data) {
        float currentPercent = data.getTechniques().getTechniqueChargePercent();
        if (currentPercent < DMZ_OVERCHARGE_MAX_PERCENT && proposedPercent > DMZ_OVERCHARGE_MAX_PERCENT) {
            proposedPercent = DMZ_OVERCHARGE_MAX_PERCENT + ((proposedPercent - DMZ_OVERCHARGE_MAX_PERCENT) * 0.5F);
        } else if (currentPercent >= DMZ_OVERCHARGE_MAX_PERCENT) {
            proposedPercent = currentPercent + ((proposedPercent - currentPercent) * 0.5F);
        }
        proposedPercent = applyCastReduction(currentPercent, proposedPercent, data, targetPercent);
        return Math.min(targetPercent, proposedPercent);
    }

    public static float applyOverchargeCastReduction(float currentPercent, float proposedPercent, StatsData data, float maxPercent) {
        return applyCastReduction(currentPercent, proposedPercent, data, maxPercent);
    }

    public static float applyCastReduction(float currentPercent, float proposedPercent, StatsData data, float maxPercent) {
        if (proposedPercent <= currentPercent || proposedPercent <= NORMAL_MAX_PERCENT) {
            if (proposedPercent <= currentPercent) {
                return proposedPercent;
            }
        }

        float step = proposedPercent - currentPercent;
        if (step <= 0.0F) {
            return proposedPercent;
        }

        double reduction = DmzRevampHelper.getKiOverchargeCastTimeReduction(data);
        double multiplier = reduction > 0D ? 1D / Math.max(0.0001D, 1D - reduction) : 1D;
        KiAttackData chargingTechnique = getChargingKiAttack(data);
        multiplier *= ClassSkillEvents.kiAssassinCastStepMultiplier(data, chargingTechnique);
        if (multiplier <= 1.000001D) {
            return Math.min(maxPercent, proposedPercent);
        }

        return Math.min(maxPercent, currentPercent + (step * (float) multiplier));
    }

    private static KiAttackData getChargingKiAttack(StatsData data) {
        if (data == null || data.getTechniques() == null) {
            return null;
        }
        String techniqueId = data.getTechniques().getChargingTechniqueId();
        if (techniqueId == null || techniqueId.isEmpty()) {
            TechniqueData selected = data.getTechniques().getSelectedTechnique();
            return selected instanceof KiAttackData kiAttack ? kiAttack : null;
        }
        TechniqueData technique = data.getTechniques().getUnlockedTechniques().get(techniqueId);
        return technique instanceof KiAttackData kiAttack ? kiAttack : null;
    }

    // Returns the Ki budget available for ki attack charge costs.
    public static float availableTechniquePayment(Resources resources, ServerPlayer player, StatsData data) {
        return resources.getCurrentEnergy();
    }

    // Pays a held charge cost with Ki only.
    public static void payTechniqueCost(Resources resources, float requestedCost, ServerPlayer player, StatsData data) {
        float remaining = Math.max(0.0F, requestedCost);
        if (remaining <= 0.0F) {
            return;
        }

        float energySpent = Math.min(resources.getCurrentEnergy(), remaining);
        if (energySpent > 0.0F) {
            resources.removeEnergy(energySpent);
            ClassSkillEvents.onKiActionSpent(player, data, energySpent);
        }

        NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
    }

    // Pauses overcharge when the player cannot pay the next overcharge tick with Ki.
    public static void drainAllTechniquePayment(Resources resources, ServerPlayer player, StatsData data) {
        if (data.getTechniques().getTechniqueChargePercent() >= NORMAL_MAX_PERCENT) {
            player.getPersistentData().putBoolean(OVERCHARGE_PAUSED_TAG, true);
            return;
        }
        resources.setCurrentEnergy(0.0F);
    }

    public static void updateChargePercent(Resources resources, ServerPlayer player, StatsData data, float nextPercent) {
        data.getTechniques().setTechniqueChargePercent(resolveChargePercentUpdate(player, data, nextPercent));
    }

    public static float resolveChargePercentUpdate(ServerPlayer player, StatsData data, float nextPercent) {
        if (player.getPersistentData().getBoolean(OVERCHARGE_PAUSED_TAG)) {
            player.getPersistentData().remove(OVERCHARGE_PAUSED_TAG);
            return Math.max(NORMAL_MAX_PERCENT, data.getTechniques().getTechniqueChargePercent());
        }
        return nextPercent;
    }

    // Pays the release cost with Ki first, then half the missing amount from HP.
    public static void payReleaseCost(Resources resources, float requestedCost, ServerPlayer player, StatsData data) {
        float remaining = Math.max(0.0F, requestedCost);
        if (remaining <= 0.0F) {
            return;
        }
        float energySpent = Math.min(resources.getCurrentEnergy(), remaining);
        if (energySpent > 0.0F) {
            resources.removeEnergy(energySpent);
            ClassSkillEvents.onKiActionSpent(player, data, energySpent);
            remaining -= energySpent;
        }
        if (remaining > 0.0F) {
            player.setHealth(Math.max(0.0F, player.getHealth() - (remaining * 0.5F)));
        }
        NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
    }

    // Converts TechniqueDispatcher's charge multiplier back to a visual charge percent.
    public static float chargePercentFromMultiplier(float chargeMultiplier) {
        return Math.max(0.0F, chargeMultiplier * 100.0F);
    }

    public static float clampChargePercent(float chargePercent) {
        if (!Float.isFinite(chargePercent)) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(maxChargePercent(), chargePercent));
    }

    public static boolean isVisuallyOverloaded(float chargePercent) {
        return clampChargePercent(chargePercent) > VISUAL_OVERLOAD_START_PERCENT;
    }

    // Returns the projectile size multiplier for the overloaded charge range.
    public static float projectileSizeMultiplier(float chargeMultiplier) {
        float percent = clampChargePercent(chargePercentFromMultiplier(chargeMultiplier));
        if (percent <= VISUAL_OVERLOAD_START_PERCENT) {
            return 1.0F;
        }
        float progress = overchargeProgress(percent);
        float configuredCap = (float) Math.max(1.0D, DmzRevampConfig.KI_OVERCHARGE_DESTRUCTION_MULTIPLIER_CAP.get());
        return 1.0F + progress * (configuredCap - 1.0F);
    }

    public static float destructionMultiplier(float chargePercent) {
        float percent = clampChargePercent(chargePercent);
        if (percent <= VISUAL_OVERLOAD_START_PERCENT) {
            return 1.0F;
        }
        float progress = overchargeProgress(percent);
        return 1.0F + progress;
    }

    public static double capOverchargeDestructionMultiplier(double multiplier) {
        if (!Double.isFinite(multiplier)) {
            return 1D;
        }
        double cap = Math.max(1D, DmzRevampConfig.KI_OVERCHARGE_DESTRUCTION_MULTIPLIER_CAP.get());
        return Math.min(multiplier, cap);
    }

    public static int capOverchargeCooldownToNormalDmzRelease(KiAttackData attack, float chargeMultiplier, int cooldownTicks) {
        if (DmzKiOverchargeCompat.isLoaded()
                || !DmzRevampConfig.CAP_KI_OVERCHARGE_COOLDOWN_TO_NORMAL_DMZ_RELEASE.get()
                || attack == null
                || chargeMultiplier <= (DMZ_OVERCHARGE_MAX_PERCENT / 100.0F)
                || cooldownTicks <= 1) {
            return cooldownTicks;
        }
        int normalReleaseCooldown = Math.max(1, (int) Math.ceil(attack.getActualCooldown() * (DMZ_OVERCHARGE_MAX_PERCENT / 100.0F)));
        return Math.min(cooldownTicks, normalReleaseCooldown);
    }

    // Returns the HUD fill ratio for the second overcharge range.
    public static float secondOverchargeFill(float chargePercent) {
        if (chargePercent <= DMZ_OVERCHARGE_MAX_PERCENT) {
            return 0.0F;
        }
        return overchargeProgress(chargePercent);
    }

    private static float overchargeProgress(float chargePercent) {
        float range = Math.max(1.0F, maxChargePercent() - VISUAL_OVERLOAD_START_PERCENT);
        return Math.min(1.0F, (chargePercent - VISUAL_OVERLOAD_START_PERCENT) / range);
    }
}
