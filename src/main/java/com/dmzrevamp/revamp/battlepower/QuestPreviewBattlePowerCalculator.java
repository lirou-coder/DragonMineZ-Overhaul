package com.dmzrevamp.revamp.battlepower;

import com.dmzrevamp.config.CustomBattlePowerConfig;
import com.dmzrevamp.revamp.quest.QuestMobEffectConfig;
import com.dragonminez.common.quest.Difficulty;
import com.dragonminez.common.quest.Quest;
import com.dragonminez.common.quest.QuestObjective;
import com.dragonminez.common.quest.objectives.KillObjective;
import com.dragonminez.common.stats.StatsData;
import com.dmzrevamp.revamp.prestige.PrestigeSystem;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.List;
import java.util.Map;

public final class QuestPreviewBattlePowerCalculator {
    private QuestPreviewBattlePowerCalculator() {
    }

    public static long calculate(Quest quest, KillObjective objective, Difficulty difficulty, int partySize, LivingEntity previewEntity, StatsData viewerStats) {
        if (quest == null || objective == null) {
            return 0L;
        }

        Difficulty selectedDifficulty = difficulty == null ? Difficulty.NORMAL : difficulty;
        int scaledPartySize = Math.max(1, partySize);
        double prestigeDifficulty = viewerStats == null ? 1D : PrestigeSystem.storyDifficultyMultiplier(viewerStats);
        double healthDifficulty = PrestigeSystem.roundedDifficultyValue(selectedDifficulty.hpMultiplier() * prestigeDifficulty);
        double damageDifficulty = PrestigeSystem.roundedDifficultyValue(selectedDifficulty.damageMultiplier() * prestigeDifficulty);
        double health = quest.getScaledKillHealth(objective, scaledPartySize) * healthDifficulty;
        double meleeDamage = quest.getScaledKillMeleeDamage(objective, scaledPartySize) * damageDifficulty;
        double kiDamage = quest.getScaledKillKiDamage(objective, scaledPartySize) * damageDifficulty;
        int killIndex = killIndex(quest, objective);
        QuestPreviewExtraStatsResolver.ExtraStats extraStats = QuestPreviewExtraStatsResolver.resolve(quest, objective, killIndex);

        CustomBattlePowerConfig.Config config = CustomBattlePowerConfig.get();
        double total = 0D;
        total += CustomBattlePowerConfig.weightedValue(config.mobStats, "maxHealth", health);
        total += CustomBattlePowerConfig.weightedValue(config.mobStats, "attackDamage", meleeDamage);
        total += weightedMobValue(config.mobStats, "kiDamage", "kiBlastDamage", kiDamage);
        total += CustomBattlePowerConfig.weightedValue(config.mobStats, "armor", questArmor(extraStats, previewEntity));
        total += CustomBattlePowerConfig.weightedValue(config.mobStats, "armorToughness", questArmorToughness(extraStats, previewEntity));
        total += CustomBattlePowerConfig.weightedValue(config.mobStats, "protection", questProtection(extraStats));
        total += CustomBattlePowerConfig.weightedValue(config.mobStats, "resistance", questResistancePower(extraStats, previewEntity));
        total += CustomBattlePowerConfig.weightedValue(config.mobStats, "movementOrFlyingSpeed", questMovementSpeed(extraStats, previewEntity));

        if (!Double.isFinite(total) || total <= 0D) {
            return 0L;
        }
        return CustomBattlePowerCalculator.calculateMobBattlePower(total);
    }

    private static double weightedMobValue(Map<String, CustomBattlePowerConfig.StatRule> rules, String key, String legacyKey, double value) {
        if (rules != null && rules.containsKey(key)) {
            return CustomBattlePowerConfig.weightedValue(rules, key, value);
        }
        return CustomBattlePowerConfig.weightedValue(rules, legacyKey, value);
    }

    public static QuestPreviewExtraStatsResolver.ExtraStats extraStats(Quest quest, KillObjective objective) {
        return QuestPreviewExtraStatsResolver.resolve(quest, objective, killIndex(quest, objective));
    }

    private static double questArmor(QuestPreviewExtraStatsResolver.ExtraStats extraStats, LivingEntity previewEntity) {
        if (extraStats.armor != null) {
            return extraStats.armor;
        }
        return attributeValue(previewEntity, Attributes.ARMOR);
    }

    private static double questArmorToughness(QuestPreviewExtraStatsResolver.ExtraStats extraStats, LivingEntity previewEntity) {
        if (extraStats.armorToughness != null) {
            return extraStats.armorToughness;
        }
        return attributeValue(previewEntity, Attributes.ARMOR_TOUGHNESS);
    }

    private static double questProtection(QuestPreviewExtraStatsResolver.ExtraStats extraStats) {
        if (extraStats.protection != null) {
            return extraStats.protection;
        }
        return 0D;
    }

    private static double questMovementSpeed(QuestPreviewExtraStatsResolver.ExtraStats extraStats, LivingEntity previewEntity) {
        if (extraStats.movementSpeed != null) {
            return extraStats.movementSpeed;
        }
        return attributeValue(previewEntity, Attributes.MOVEMENT_SPEED);
    }

    private static double questResistancePower(QuestPreviewExtraStatsResolver.ExtraStats extraStats, LivingEntity previewEntity) {
        double configuredResistance = resistancePower(extraStats.mobEffects);
        if (configuredResistance > 0D) {
            return configuredResistance;
        }

        if (previewEntity == null) {
            return 0D;
        }
        MobEffectInstance resistance = previewEntity.getEffect(MobEffects.DAMAGE_RESISTANCE);
        return resistance == null ? 0D : (resistance.getAmplifier() + 1D) * 4D;
    }

    private static double resistancePower(List<QuestMobEffectConfig> effects) {
        if (effects == null || effects.isEmpty()) {
            return 0D;
        }
        double strongest = 0D;
        for (QuestMobEffectConfig effect : effects) {
            if (effect == null || effect.effectId() == null) {
                continue;
            }
            String id = effect.effectId().toLowerCase();
            if (id.equals("resistance") || id.equals("minecraft:resistance") || id.equals("damage_resistance") || id.equals("minecraft:damage_resistance")) {
                strongest = Math.max(strongest, (effect.amplifier() + 1D) * 4D);
            }
        }
        return strongest;
    }

    private static int killIndex(Quest quest, KillObjective objective) {
        if (quest == null || objective == null) {
            return -1;
        }
        int index = 0;
        for (QuestObjective questObjective : quest.getObjectives()) {
            if (questObjective instanceof KillObjective killObjective) {
                if (killObjective == objective) {
                    return index;
                }
                index++;
            }
        }
        return -1;
    }

    private static double attributeValue(LivingEntity entity, net.minecraft.world.entity.ai.attributes.Attribute attribute) {
        if (entity == null) {
            return 0D;
        }
        AttributeInstance instance = entity.getAttribute(attribute);
        return instance == null ? 0D : instance.getValue();
    }
}
