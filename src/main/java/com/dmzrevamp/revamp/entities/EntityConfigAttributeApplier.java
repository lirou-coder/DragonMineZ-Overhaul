package com.dmzrevamp.revamp.entities;

import com.dmzrevamp.revamp.quest.QuestMobEffectConfig;
import com.dmzrevamp.revamp.quest.QuestSpawnAttributeApplier;
import com.dmzrevamp.revamp.quest.TransformStageOverridesWriter;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.EntitiesConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

public final class EntityConfigAttributeApplier {
    private static final String KI_DAMAGE_TAG = "kiDamage";

    private EntityConfigAttributeApplier() {
    }

    public static void apply(LivingEntity entity) {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (key == null) {
            return;
        }

        EntitiesConfig.EntityStats stats = ConfigManager.getEntityStats(key.toString());
        EntitiesConfig.TransformSettings defaults = ConfigManager.getEntityTransformDefaults();
        RevampEntityStatsData entityData = stats instanceof RevampEntityStatsData data ? data : null;
        RevampTransformSettingsData defaultData = defaults instanceof RevampTransformSettingsData data ? data : null;
        if (stats == null && defaultData == null) {
            return;
        }

        CompoundTag tag = entity.getPersistentData();
        applyBaseFields(entity, tag, stats, entityData);
        applyTransformFields(tag, entityData, defaultData);
        TransformStageOverridesWriter.save(tag, TransformChainConfig.get(key.toString(), 2), 2, false, 1D, 1D);
        TransformStageOverridesWriter.save(tag, TransformChainConfig.get(key.toString(), 3), 3, false, 1D, 1D);
    }

    private static void applyBaseFields(LivingEntity entity, CompoundTag tag, EntitiesConfig.EntityStats stats, RevampEntityStatsData data) {
        if (stats != null && stats.getKiDamage() != null && !tag.contains(KI_DAMAGE_TAG)) {
            tag.putDouble(KI_DAMAGE_TAG, stats.getKiDamage());
        }
        if (data == null) {
            return;
        }

        // These tags reuse the quest applier so entities.json and saga quests affect mobs in the same way.
        putIfAbsent(tag, QuestSpawnAttributeApplier.ARMOR_TAG, data.dmzrevamp$getArmor());
        putIfAbsent(tag, QuestSpawnAttributeApplier.ARMOR_TOUGHNESS_TAG, data.dmzrevamp$getArmorToughness());
        putIfAbsent(tag, QuestSpawnAttributeApplier.PROTECTION_TAG, data.dmzrevamp$getProtection());
        putIfAbsent(tag, QuestSpawnAttributeApplier.MOVEMENT_SPEED_TAG, data.dmzrevamp$getMovementSpeed());
        if (data.dmzrevamp$getArmor() != null) {
            tag.putBoolean(QuestSpawnAttributeApplier.ARMOR_CONFIGURED_TAG, true);
        }
        saveEffectsIfAbsent(tag, QuestSpawnAttributeApplier.MOB_EFFECTS_TAG, data.dmzrevamp$getMobEffects());
        QuestSpawnAttributeApplier.applyConfiguredSpawnAttributes(entity);
        QuestSpawnAttributeApplier.applyMobEffects(entity);
    }

    private static void applyTransformFields(CompoundTag tag, RevampEntityStatsData entityData, RevampTransformSettingsData defaultData) {
        putDefaultPair(tag, QuestSpawnAttributeApplier.TF_ARMOR_TAG, QuestSpawnAttributeApplier.TF_ARMOR_MULT_TAG,
                entityData == null ? null : entityData.dmzrevamp$getTransformArmor(),
                entityData == null ? null : entityData.dmzrevamp$getTransformArmorMultiplier(),
                defaultData == null ? null : defaultData.dmzrevamp$getTransformArmor(),
                defaultData == null ? null : defaultData.dmzrevamp$getTransformArmorMultiplier());
        putDefaultPair(tag, QuestSpawnAttributeApplier.TF_ARMOR_TOUGHNESS_TAG, QuestSpawnAttributeApplier.TF_ARMOR_TOUGHNESS_MULT_TAG,
                entityData == null ? null : entityData.dmzrevamp$getTransformArmorToughness(),
                entityData == null ? null : entityData.dmzrevamp$getTransformArmorToughnessMultiplier(),
                defaultData == null ? null : defaultData.dmzrevamp$getTransformArmorToughness(),
                defaultData == null ? null : defaultData.dmzrevamp$getTransformArmorToughnessMultiplier());
        putDefaultPair(tag, QuestSpawnAttributeApplier.TF_PROTECTION_TAG, QuestSpawnAttributeApplier.TF_PROTECTION_MULT_TAG,
                entityData == null ? null : entityData.dmzrevamp$getTransformProtection(),
                entityData == null ? null : entityData.dmzrevamp$getTransformProtectionMultiplier(),
                defaultData == null ? null : defaultData.dmzrevamp$getTransformProtection(),
                defaultData == null ? null : defaultData.dmzrevamp$getTransformProtectionMultiplier());
        putDefaultPair(tag, QuestSpawnAttributeApplier.TF_MOVEMENT_SPEED_TAG, QuestSpawnAttributeApplier.TF_MOVEMENT_SPEED_MULT_TAG,
                entityData == null ? null : entityData.dmzrevamp$getTransformMovementSpeed(),
                entityData == null ? null : entityData.dmzrevamp$getTransformMovementSpeedMultiplier(),
                defaultData == null ? null : defaultData.dmzrevamp$getTransformMovementSpeed(),
                defaultData == null ? null : defaultData.dmzrevamp$getTransformMovementSpeedMultiplier());

        List<QuestMobEffectConfig> effects = firstNonEmpty(
                entityData == null ? List.of() : entityData.dmzrevamp$getTransformMobEffects(),
                defaultData == null ? List.of() : defaultData.dmzrevamp$getTransformMobEffects()
        );
        saveEffectsIfAbsent(tag, QuestSpawnAttributeApplier.TF_MOB_EFFECTS_TAG, effects);
    }

    private static void putIfAbsent(CompoundTag tag, String key, Double value) {
        if (value != null && !tag.contains(key)) {
            tag.putDouble(key, value);
        }
    }

    private static void putDefaultPair(CompoundTag tag, String exactKey, String multiplierKey,
                                       Double entityExact, Double entityMultiplier,
                                       Double globalExact, Double globalMultiplier) {
        // A quest value already stored in either key owns the whole pair. Likewise, an
        // entity-specific pair replaces the global pair instead of being mixed with it.
        if (tag.contains(exactKey) || tag.contains(multiplierKey)) return;
        boolean hasEntityValue = entityExact != null || entityMultiplier != null;
        Double exact = hasEntityValue ? entityExact : globalExact;
        Double multiplier = hasEntityValue ? entityMultiplier : globalMultiplier;
        if (exact != null) tag.putDouble(exactKey, exact);
        else if (multiplier != null) tag.putDouble(multiplierKey, multiplier);
    }

    private static void saveEffectsIfAbsent(CompoundTag tag, String key, List<QuestMobEffectConfig> effects) {
        if (!tag.contains(key, 9)) {
            QuestSpawnAttributeApplier.saveMobEffects(tag, key, effects);
        }
    }

    private static Double first(Double primary, Double fallback) {
        return primary != null ? primary : fallback;
    }

    private static List<QuestMobEffectConfig> firstNonEmpty(List<QuestMobEffectConfig> primary, List<QuestMobEffectConfig> fallback) {
        return primary != null && !primary.isEmpty() ? primary : fallback;
    }
}
