package com.dmzrevamp.revamp.fusion;

import com.dmzrevamp.config.FusionsRevampedConfig;
import com.dmzrevamp.revamp.prestige.PrestigeSystem;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceStatsConfig;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ProgressionSyncS2C;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.MainEffects;
import net.minecraft.world.effect.MobEffectInstance;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.BonusStats;
import com.dragonminez.common.stats.character.Resources;
import com.dragonminez.server.util.FusionLogic;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

public final class FusionRevampLogic {
    public static final String BONUS_NAME = "Fusion";
    private static final String MIRRORED_BONUS_PREFIX = "Fusion ";
    private static final String[] BONUS_STATS = {"STR", "SKP", "DEF", "STM", "VIT", "PWR", "ENE"};
    private static final String SCALE_TAG = "DmzRevampFusionScale";
    private static final String MULTIPLIER_TAG = "DmzRevampFusionMultipliers";
    private static final String[] MIRRORED_MULTIPLIER_STATS = {"STR", "SKP", "DEF", "STM", "VIT", "PWR", "ENE"};

    private FusionRevampLogic() {
    }

    public static void applyFusionBonuses(StatsData leaderData, StatsData partnerData, String fusionType, int leaderTotalStats, int partnerTotalStats) {
        clearFusionBonuses(leaderData);
        clearFusionBonuses(partnerData);
        if (!FusionsRevampedConfig.isRevampedEnabled() || leaderData == null || partnerData == null) {
            return;
        }

        FusionsRevampedConfig.FusionRevamped config = FusionsRevampedConfig.get().fusionRevamped;
        double similarity = similarity(leaderTotalStats, partnerTotalStats);
        double minBonus = "POTHALA".equalsIgnoreCase(fusionType) ? config.potaraMinBonus : config.metamoruMinBonus;
        double maxBonus = "POTHALA".equalsIgnoreCase(fusionType) ? config.potaraMaxBonus : config.metamoruMaxBonus;
        double bonusRatio = minBonus + (similarity * (maxBonus - minBonus));

        BonusStats leaderBonuses = leaderData.getBonusStats();
        BonusStats partnerBonuses = partnerData.getBonusStats();
        for (String rawStat : config.fusionBoosts) {
            String stat = FusionsRevampedConfig.normalizeStat(rawStat);
            int leaderValue = getBaseStatValue(leaderData, stat);
            int partnerValue = getBaseStatValue(partnerData, stat);
            if (partnerValue > 0) {
                // Fusion adds a flat portion of the partner's base stat before forms and multipliers are calculated.
                leaderBonuses.addBonusSplit(stat, BONUS_NAME, "+", partnerValue * bonusRatio, true);
            }

            double leaderTarget = leaderValue + (partnerValue * bonusRatio);
            double partnerBonus = leaderTarget - partnerValue;
            if (partnerBonus != 0D) {
                // The observing partner mirrors the controller's fused stat so both stat menus show the same result.
                partnerBonuses.addBonusSplit(stat, BONUS_NAME, "+", partnerBonus, true);
            }
        }
        writeFusionScaleAdditions(leaderData, partnerData, config);
        mirrorLeaderMultipliersToObserver(leaderData, partnerData);
        syncProgression(leaderData);
        syncProgression(partnerData);
    }

    public static void clearFusionBonuses(StatsData data) {
        if (data == null) {
            return;
        }
        data.getBonusStats().removeAllBonuses(BONUS_NAME);
        clearMirroredBonuses(data);
        CompoundTag originalAppearance = data.getStatus().getOriginalAppearance();
        if (originalAppearance != null) {
            originalAppearance.remove(SCALE_TAG);
            originalAppearance.remove(MULTIPLIER_TAG);
        }
    }

    public static double addPartnerScale(StatsData data, String stat, double originalScale) {
        double ownPrestigeScale = data == null ? originalScale : originalScale * PrestigeSystem.scaleMultiplier(data);
        FusionsRevampedConfig.Config config = FusionsRevampedConfig.get();
        if (!config.fusionRevamped.enabled || !config.fusionRevamped.scaleAddition || data == null || data.getPlayer() == null) {
            return ownPrestigeScale;
        }

        String normalizedStat = FusionsRevampedConfig.normalizeStat(stat);
        if (!config.fusionRevamped.boosts(normalizedStat) || !data.getStatus().isFused()) {
            return ownPrestigeScale;
        }

        double storedScaleAddition = getStoredScaleAddition(data, normalizedStat);
        if (storedScaleAddition > 0D) {
            return ownPrestigeScale + storedScaleAddition;
        }

        StatsData partnerData = getFusionPartnerData(data);
        return partnerData == null ? ownPrestigeScale
                : ownPrestigeScale + getBaseRaceClassScale(partnerData, normalizedStat) * PrestigeSystem.scaleMultiplier(partnerData);
    }

    public static double mirroredTotalMultiplierOrOriginal(StatsData data, String stat, double originalMultiplier) {
        if (data == null || !data.getStatus().isFused() || data.getStatus().isFusionLeader()) {
            return originalMultiplier;
        }

        CompoundTag originalAppearance = data.getStatus().getOriginalAppearance();
        if (originalAppearance == null || !originalAppearance.contains(MULTIPLIER_TAG)) {
            return originalMultiplier;
        }
        CompoundTag multipliers = originalAppearance.getCompound(MULTIPLIER_TAG);
        String normalizedStat = FusionsRevampedConfig.normalizeStat(stat);
        return multipliers.contains(normalizedStat) ? multipliers.getDouble(normalizedStat) : originalMultiplier;
    }

    public static boolean mirrorLeaderMultipliersToObserver(StatsData leaderData, StatsData observerData) {
        if (leaderData == null || observerData == null || !leaderData.getStatus().isFusionLeader() || observerData.getStatus().isFusionLeader()) {
            return false;
        }
        CompoundTag originalAppearance = observerData.getStatus().getOriginalAppearance();
        if (originalAppearance == null) {
            originalAppearance = new CompoundTag();
            observerData.getStatus().setOriginalAppearance(originalAppearance);
        }
        CompoundTag existing = originalAppearance.contains(MULTIPLIER_TAG)
                ? originalAppearance.getCompound(MULTIPLIER_TAG) : null;
        boolean changed = existing == null;
        if (!changed) {
            for (String stat : MIRRORED_MULTIPLIER_STATS) {
                double value = leaderData.getTotalMultiplier(stat);
                if (!existing.contains(stat) || Math.abs(existing.getDouble(stat) - value) > 0.0000001D) {
                    changed = true;
                    break;
                }
            }
        }
        if (!changed) {
            return false;
        }
        CompoundTag multipliers = new CompoundTag();
        for (String stat : MIRRORED_MULTIPLIER_STATS) {
            // The observer stores the controller's current form/stack/effect multiplier for stat menu calculations.
            multipliers.putDouble(stat, leaderData.getTotalMultiplier(stat));
        }
        originalAppearance.put(MULTIPLIER_TAG, multipliers);
        return true;
    }

    public static int finishFusion(ServerPlayer player, boolean forced) {
        if (player == null) {
            return 0;
        }
        StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
        if (data == null || (!data.getStatus().isFused() && data.getStatus().getFusionPartnerUUID() == null)) {
            return 0;
        }
        UUID partnerId = data.getStatus().getFusionPartnerUUID();
        ServerPlayer partner = partnerId == null ? null : player.server.getPlayerList().getPlayer(partnerId);
        StatsData partnerData = partner == null ? null : StatsProvider.get(StatsCapability.INSTANCE, partner).orElse(null);
        if (forced) {
            // DMZ resolves the live Curios head_tech stack and consumes the
            // equipped Pothala without depending on a cached inventory copy.
            FusionLogic.breakPothala(player);
            if (partner != null) {
                FusionLogic.breakPothala(partner);
            }
        }
        com.dragonminez.server.util.FusionLogic.endFusion(player, data, forced);
        if (forced) {
            int cooldownTicks = Math.max(1, ConfigManager.getServerConfig().getGameplay().getFusionCooldownSeconds() * 20);
            applyFusionCooldown(player, data, cooldownTicks);
            applyFusionCooldown(partner, partnerData, cooldownTicks);
        }
        return 1;
    }

    private static void applyFusionCooldown(ServerPlayer player, StatsData data, int ticks) {
        if (player == null || data == null) return;
        data.getCooldowns().addCooldown("FusionCooldown", ticks);
        player.addEffect(new MobEffectInstance(MainEffects.FUSION_CD.get(), ticks, 0, false, false, true));
        NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
    }

    public static boolean synchronizeSharedFusionState(StatsData leaderData, StatsData observerData) {
        if (leaderData == null || observerData == null
                || !leaderData.getStatus().isFused() || !leaderData.getStatus().isFusionLeader()) {
            return false;
        }
        Player leader = leaderData.getPlayer();
        Player observer = observerData.getPlayer();
        if (leader == null || observer == null) {
            return false;
        }

        boolean changed = false;
        float sharedHealth = Math.max(0F, Math.min(observer.getMaxHealth(), leader.getHealth()));
        if (Math.abs(observer.getHealth() - sharedHealth) > 0.0001F) {
            observer.setHealth(sharedHealth);
            changed = true;
        }

        Resources leaderResources = leaderData.getResources();
        Resources observerResources = observerData.getResources();
        float sharedEnergy = Math.max(0F, Math.min(observerData.getMaxEnergy(), leaderResources.getCurrentEnergy()));
        if (Math.abs(observerResources.getCurrentEnergy() - sharedEnergy) > 0.0001F) {
            observerResources.setCurrentEnergy(sharedEnergy);
            changed = true;
        }
        return changed;
    }

    public static boolean mirrorBonusesForPair(StatsData first, StatsData second) {
        if (first == null || second == null) {
            return false;
        }
        boolean changed = mirrorBonuses(first, second);
        changed |= mirrorBonuses(second, first);
        return changed;
    }

    /** Signature of non-fusion bonuses; avoids rebuilding mirrored lists while their sources are unchanged. */
    public static long ownBonusSignature(StatsData data) {
        if (data == null) {
            return 0L;
        }
        long hash = 0xcbf29ce484222325L;
        for (String stat : BONUS_STATS) {
            hash = mix(hash, stat.hashCode());
            for (BonusStats.StatBonus bonus : data.getBonusStats().getBonuses(stat)) {
                if (BONUS_NAME.equals(bonus.name) || bonus.name.startsWith(MIRRORED_BONUS_PREFIX)) {
                    continue;
                }
                hash = mix(hash, bonus.name.hashCode());
                hash = mix(hash, bonus.operation.hashCode());
                hash = mix(hash, Double.doubleToLongBits(bonus.value));
                hash = mix(hash, bonus.applyMultipliers ? 1L : 0L);
            }
        }
        return hash;
    }

    private static long mix(long hash, long value) {
        return (hash ^ value) * 0x100000001b3L;
    }

    private static boolean mirrorBonuses(StatsData source, StatsData target) {
        boolean changed = false;
        BonusStats sourceBonuses = source.getBonusStats();
        BonusStats targetBonuses = target.getBonusStats();
        for (String stat : BONUS_STATS) {
            Map<String, List<BonusStats.StatBonus>> desired = new LinkedHashMap<>();
            for (BonusStats.StatBonus bonus : sourceBonuses.getBonuses(stat)) {
                if (BONUS_NAME.equals(bonus.name) || bonus.name.startsWith(MIRRORED_BONUS_PREFIX)) continue;
                desired.computeIfAbsent(MIRRORED_BONUS_PREFIX + bonus.name, ignored -> new java.util.ArrayList<>()).add(bonus);
            }

            List<String> existingNames = targetBonuses.getBonuses(stat).stream()
                    .map(bonus -> bonus.name)
                    .filter(name -> name.startsWith(MIRRORED_BONUS_PREFIX))
                    .distinct()
                    .toList();
            for (String existingName : existingNames) {
                if (!desired.containsKey(existingName)) {
                    targetBonuses.removeBonus(stat, existingName);
                    changed = true;
                }
            }

            for (Map.Entry<String, List<BonusStats.StatBonus>> entry : desired.entrySet()) {
                String fusionName = entry.getKey();
                List<BonusStats.StatBonus> sourceEntries = entry.getValue();
                List<BonusStats.StatBonus> existingEntries = targetBonuses.getBonuses(stat).stream()
                        .filter(bonus -> fusionName.equals(bonus.name))
                        .toList();
                if (sameBonuses(sourceEntries, existingEntries)) {
                    continue;
                }
                targetBonuses.removeBonus(stat, fusionName);
                for (BonusStats.StatBonus bonus : sourceEntries) {
                    targetBonuses.addBonus(stat, fusionName, bonus.operation, bonus.value, bonus.applyMultipliers);
                }
                changed = true;
            }
        }
        return changed;
    }

    private static boolean sameBonuses(List<BonusStats.StatBonus> source, List<BonusStats.StatBonus> mirrored) {
        if (source.size() != mirrored.size()) {
            return false;
        }
        for (int i = 0; i < source.size(); i++) {
            BonusStats.StatBonus expected = source.get(i);
            BonusStats.StatBonus actual = mirrored.get(i);
            if (!expected.operation.equals(actual.operation)
                    || Math.abs(expected.value - actual.value) > 0.0000001D
                    || expected.applyMultipliers != actual.applyMultipliers) {
                return false;
            }
        }
        return true;
    }

    private static void clearMirroredBonuses(StatsData data) {
        for (String stat : BONUS_STATS) {
            List<String> mirroredNames = data.getBonusStats().getBonuses(stat).stream()
                    .map(bonus -> bonus.name)
                    .filter(name -> name.startsWith(MIRRORED_BONUS_PREFIX))
                    .distinct()
                    .toList();
            for (String mirroredName : mirroredNames) {
                data.getBonusStats().removeBonus(stat, mirroredName);
            }
        }
    }

    public static void restoreFusionResources(StatsData leaderData, StatsData partnerData,
                                              double healthPercent, double energyPercent, double staminaPercent,
                                              boolean restoreHealth) {
        if (leaderData == null || partnerData == null) {
            return;
        }
        setResourcePercent(leaderData, healthPercent, energyPercent, staminaPercent, restoreHealth);
        setResourcePercent(partnerData, healthPercent, energyPercent, staminaPercent, restoreHealth);
        syncProgression(leaderData);
        syncProgression(partnerData);
    }

    private static void setResourcePercent(StatsData data, double healthPercent, double energyPercent,
                                           double staminaPercent, boolean restoreHealth) {
        Resources resources = data.getResources();
        resources.setCurrentEnergy((float) (data.getMaxEnergy() * energyPercent));
        resources.setCurrentStamina((float) (data.getMaxStamina() * staminaPercent));
        if (restoreHealth && data.getPlayer() != null) {
            float maxHealth = data.getPlayer().getMaxHealth();
            data.getPlayer().setHealth((float) Math.max(1D, Math.min(maxHealth, data.getMaxHealth() * healthPercent)));
        }
    }

    private static void writeFusionScaleAdditions(StatsData leaderData, StatsData partnerData, FusionsRevampedConfig.FusionRevamped config) {
        CompoundTag leaderScales = new CompoundTag();
        CompoundTag partnerScales = new CompoundTag();
        for (String rawStat : config.fusionBoosts) {
            String stat = FusionsRevampedConfig.normalizeStat(rawStat);
            leaderScales.putDouble(stat, getBaseRaceClassScale(partnerData, stat) * PrestigeSystem.scaleMultiplier(partnerData));
            partnerScales.putDouble(stat, getBaseRaceClassScale(leaderData, stat) * PrestigeSystem.scaleMultiplier(leaderData));
        }
        putScaleTag(leaderData, leaderScales);
        putScaleTag(partnerData, partnerScales);
    }

    private static void putScaleTag(StatsData data, CompoundTag scales) {
        CompoundTag originalAppearance = data.getStatus().getOriginalAppearance();
        if (originalAppearance == null) {
            originalAppearance = new CompoundTag();
            data.getStatus().setOriginalAppearance(originalAppearance);
        }
        originalAppearance.put(SCALE_TAG, scales);
    }

    private static double getStoredScaleAddition(StatsData data, String stat) {
        CompoundTag originalAppearance = data.getStatus().getOriginalAppearance();
        if (originalAppearance == null || !originalAppearance.contains(SCALE_TAG)) {
            return 0D;
        }
        CompoundTag scales = originalAppearance.getCompound(SCALE_TAG);
        return scales.contains(stat) ? scales.getDouble(stat) : 0D;
    }

    public static StatsData getFusionPartnerData(StatsData data) {
        Player player = data.getPlayer();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return null;
        }
        UUID partnerId = data.getStatus().getFusionPartnerUUID();
        if (partnerId == null) {
            return null;
        }
        ServerPlayer partner = serverPlayer.server.getPlayerList().getPlayer(partnerId);
        if (partner == null) {
            return null;
        }
        return StatsProvider.get(StatsCapability.INSTANCE, partner).orElse(null);
    }

    private static double getBaseRaceClassScale(StatsData data, String stat) {
        RaceStatsConfig raceConfig = ConfigManager.getRaceStats(data.getCharacter().getRaceName());
        RaceStatsConfig.ClassStats classStats = raceConfig != null ? raceConfig.getClassStats(data.getCharacter().getCharacterClass()) : null;
        RaceStatsConfig.StatScaling scaling = classStats != null ? classStats.getStatScaling() : null;
        if (scaling == null) {
            return 1D;
        }
        Double value = switch (stat) {
            case "STR" -> scaling.getStrengthScaling();
            case "SKP" -> scaling.getStrikePowerScaling();
            case "DEF" -> scaling.getDefenseScaling();
            case "STM" -> scaling.getStaminaScaling();
            case "VIT" -> scaling.getVitalityScaling();
            case "PWR" -> scaling.getKiPowerScaling();
            case "ENE" -> scaling.getEnergyScaling();
            default -> 1D;
        };
        return value == null || !Double.isFinite(value) ? 1D : value;
    }

    private static int getBaseStatValue(StatsData data, String stat) {
        return switch (stat) {
            case "STR" -> data.getStats().getStrength();
            case "SKP" -> data.getStats().getStrikePower();
            case "DEF", "STM" -> data.getStats().getResistance();
            case "VIT" -> data.getStats().getVitality();
            case "PWR" -> data.getStats().getKiPower();
            case "ENE" -> data.getStats().getEnergy();
            default -> 0;
        };
    }

    private static double similarity(int firstTotal, int secondTotal) {
        int max = Math.max(firstTotal, secondTotal);
        if (max <= 0) {
            return 1D;
        }
        return Math.max(0D, Math.min(1D, Math.min(firstTotal, secondTotal) / (double) max));
    }

    private static void syncProgression(StatsData data) {
        if (data.getPlayer() instanceof ServerPlayer player) {
            NetworkHandler.sendToTrackingEntityAndSelf(new ProgressionSyncS2C(player), player);
            NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
        }
    }

    public static void syncFusionPair(StatsData first, StatsData second) {
        syncProgression(first);
        syncProgression(second);
    }
}
