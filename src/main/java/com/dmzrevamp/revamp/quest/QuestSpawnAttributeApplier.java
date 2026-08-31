package com.dmzrevamp.revamp.quest;

import com.dragonminez.common.quest.Quest;
import com.dragonminez.common.quest.QuestObjective;
import com.dragonminez.common.quest.QuestRegistry;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.dragonminez.common.quest.objectives.KillObjective;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.UUID;

public final class QuestSpawnAttributeApplier {
    public static final String QUEST_KEY_TAG = "dmz_quest_key";
    public static final String QUEST_OWNER_TAG = "dmz_quest_owner";
    public static final String QUEST_OBJECTIVE_INDEX_TAG = "dmz_quest_objective_index";
    public static final String QUEST_NO_TRANSFORM_TAG = "dmz_quest_no_transform";
    public static final String VERIFIED_QUEST_SPAWN_TAG = "dmzrevamp_verified_quest_spawn";
    public static final String ARMOR_TAG = "dmzrevamp_quest_armor";
    public static final String ARMOR_TOUGHNESS_TAG = "dmzrevamp_quest_armor_toughness";
    public static final String PROTECTION_TAG = "dmzrevamp_quest_protection";
    public static final String MOVEMENT_SPEED_TAG = "dmzrevamp_quest_movement_speed";
    public static final String ARMOR_CONFIGURED_TAG = "dmzrevamp_quest_armor_configured";
    public static final String TF_ARMOR_TAG = "dmzrevamp_quest_tf_armor";
    public static final String TF_ARMOR_TOUGHNESS_TAG = "dmzrevamp_quest_tf_armor_toughness";
    public static final String TF_PROTECTION_TAG = "dmzrevamp_quest_tf_protection";
    public static final String TF_MOVEMENT_SPEED_TAG = "dmzrevamp_quest_tf_movement_speed";
    public static final String TF_ARMOR_MULT_TAG = "dmzrevamp_quest_tf_armor_mult";
    public static final String TF_ARMOR_TOUGHNESS_MULT_TAG = "dmzrevamp_quest_tf_armor_toughness_mult";
    public static final String TF_PROTECTION_MULT_TAG = "dmzrevamp_quest_tf_protection_mult";
    public static final String TF_MOVEMENT_SPEED_MULT_TAG = "dmzrevamp_quest_tf_movement_speed_mult";
    public static final String MOB_EFFECTS_TAG = "dmzrevamp_quest_mob_effects";
    public static final String TF_MOB_EFFECTS_TAG = "dmzrevamp_quest_tf_mob_effects";
    public static final String TRANSFORM_STAGE_TAG = "dmzrevamp_transform_stage";
    public static final String CAN_TRANSFORM_2_TAG = "dmzrevamp_can_transform_2";
    public static final String CAN_TRANSFORM_3_TAG = "dmzrevamp_can_transform_3";
    private static final String DMZ_TF_HP_ABS = "dmz_quest_tf_hp_abs";
    private static final String DMZ_TF_HP_MULT = "dmz_quest_tf_hp_mult";
    private static final String DMZ_TF_MELEE_ABS = "dmz_quest_tf_melee_abs";
    private static final String DMZ_TF_MELEE_MULT = "dmz_quest_tf_melee_mult";
    private static final String DMZ_TF_KI_ABS = "dmz_quest_tf_ki_abs";
    private static final String DMZ_TF_KI_MULT = "dmz_quest_tf_ki_mult";
    private static final String DMZ_TF_TRIGGER = "dmz_quest_tf_trigger";

    private static final UUID BASE_MOB_ARMOR_UUID = UUID.fromString("8997635e-2835-4b54-9e97-17f8e5dc570f");

    private QuestSpawnAttributeApplier() {
    }

    public static boolean hasDmzQuestSpawnTags(Entity entity) {
        CompoundTag tag = entity.getPersistentData();
        if (!tag.contains(QUEST_KEY_TAG) || !tag.contains(QUEST_OBJECTIVE_INDEX_TAG)
                || !tag.contains(QUEST_OWNER_TAG) || tag.getString(QUEST_KEY_TAG).isBlank()
                || tag.getString(QUEST_OWNER_TAG).isBlank()) {
            return false;
        }
        Quest quest = QuestRegistry.getQuest(tag.getString(QUEST_KEY_TAG));
        int objectiveIndex = tag.getInt(QUEST_OBJECTIVE_INDEX_TAG);
        return quest != null && objectiveIndex >= 0 && objectiveIndex < quest.getObjectives().size()
                && quest.getObjectives().get(objectiveIndex) instanceof KillObjective;
    }

    public static void markVerifiedQuestSpawn(Entity entity) {
        if (hasDmzQuestSpawnTags(entity)) {
            entity.getPersistentData().putBoolean(VERIFIED_QUEST_SPAWN_TAG, true);
        }
    }

    public static boolean isVerifiedQuestSpawn(Entity entity) {
        return entity.getPersistentData().getBoolean(VERIFIED_QUEST_SPAWN_TAG)
                && hasDmzQuestSpawnTags(entity);
    }

    public static void applyFromQuestTags(Entity entity) {
        if (!(entity instanceof LivingEntity living) || !hasDmzQuestSpawnTags(entity)) {
            return;
        }

        Quest quest = QuestRegistry.getQuest(entity.getPersistentData().getString(QUEST_KEY_TAG));
        int objectiveIndex = entity.getPersistentData().getInt(QUEST_OBJECTIVE_INDEX_TAG);
        if (quest == null || objectiveIndex < 0 || objectiveIndex >= quest.getObjectives().size()) {
            return;
        }

        QuestObjective objective = quest.getObjectives().get(objectiveIndex);
        if (!(objective instanceof KillObjective killObjective) || !(killObjective instanceof RevampKillObjectiveData data)) {
            return;
        }

        CompoundTag tag = entity.getPersistentData();
        if (!killObjective.isCanTransform()) {
            tag.putBoolean(QUEST_NO_TRANSFORM_TAG, true);
            if (living instanceof DBSagasEntity sagaEntity) {
                sagaEntity.setTransformationDisabled(true);
            }
        }
        putNullable(tag, ARMOR_TAG, data.dmzrevamp$getArmor());
        putNullable(tag, ARMOR_TOUGHNESS_TAG, data.dmzrevamp$getArmorToughness());
        putNullable(tag, PROTECTION_TAG, data.dmzrevamp$getProtection());
        putNullable(tag, MOVEMENT_SPEED_TAG, data.dmzrevamp$getMovementSpeed());
        if (data.dmzrevamp$getArmor() != null) {
            tag.putBoolean(ARMOR_CONFIGURED_TAG, true);
        }
        putNullable(tag, TF_ARMOR_TAG, data.dmzrevamp$getTransformArmor());
        putNullable(tag, TF_ARMOR_TOUGHNESS_TAG, data.dmzrevamp$getTransformArmorToughness());
        putNullable(tag, TF_PROTECTION_TAG, data.dmzrevamp$getTransformProtection());
        putNullable(tag, TF_MOVEMENT_SPEED_TAG, data.dmzrevamp$getTransformMovementSpeed());
        putNullable(tag, TF_ARMOR_MULT_TAG, data.dmzrevamp$getTransformArmorMultiplier());
        putNullable(tag, TF_ARMOR_TOUGHNESS_MULT_TAG, data.dmzrevamp$getTransformArmorToughnessMultiplier());
        putNullable(tag, TF_PROTECTION_MULT_TAG, data.dmzrevamp$getTransformProtectionMultiplier());
        putNullable(tag, TF_MOVEMENT_SPEED_MULT_TAG, data.dmzrevamp$getTransformMovementSpeedMultiplier());
        saveMobEffects(tag, data.dmzrevamp$getMobEffects());
        saveMobEffects(tag, TF_MOB_EFFECTS_TAG, data.dmzrevamp$getTransformMobEffects());
        TransformStageOverridesWriter.save(tag, data.dmzrevamp$getTransformStage(2), 2, true, 1D, 1D);
        TransformStageOverridesWriter.save(tag, data.dmzrevamp$getTransformStage(3), 3, true, 1D, 1D);
        tag.putBoolean(CAN_TRANSFORM_2_TAG, data.dmzrevamp$canTransformStage(2));
        tag.putBoolean(CAN_TRANSFORM_3_TAG, data.dmzrevamp$canTransformStage(3));

        applyConfiguredSpawnAttributes(living);
        applyMobEffects(living);
    }

    public static void applyConfiguredSpawnAttributes(LivingEntity entity) {
        CompoundTag tag = entity.getPersistentData();
        if (tag.contains(ARMOR_TAG)) {
            setAttributeBase(entity, Attributes.ARMOR, tag.getDouble(ARMOR_TAG), true);
        }
        if (tag.contains(ARMOR_TOUGHNESS_TAG)) {
            setAttributeBase(entity, Attributes.ARMOR_TOUGHNESS, tag.getDouble(ARMOR_TOUGHNESS_TAG), false);
        }
        if (tag.contains(MOVEMENT_SPEED_TAG)) {
            setAttributeBase(entity, Attributes.MOVEMENT_SPEED, tag.getDouble(MOVEMENT_SPEED_TAG), false);
        }
    }

    public static void applyTransformAttributes(LivingEntity previous, LivingEntity transformed) {
        copyRevampQuestTags(previous, transformed);

        CompoundTag source = previous.getPersistentData();
        double armor = resolveTransformValue(source, TF_ARMOR_TAG, TF_ARMOR_MULT_TAG, attributeValue(previous, Attributes.ARMOR));
        double toughness = resolveTransformValue(source, TF_ARMOR_TOUGHNESS_TAG, TF_ARMOR_TOUGHNESS_MULT_TAG, attributeValue(previous, Attributes.ARMOR_TOUGHNESS));
        double protection = resolveTransformValue(source, TF_PROTECTION_TAG, TF_PROTECTION_MULT_TAG, protectionValue(previous));
        double movementSpeed = resolveTransformValue(source, TF_MOVEMENT_SPEED_TAG, TF_MOVEMENT_SPEED_MULT_TAG, attributeValue(previous, Attributes.MOVEMENT_SPEED));

        if (!Double.isNaN(armor)) {
            transformed.getPersistentData().putBoolean(ARMOR_CONFIGURED_TAG, true);
            transformed.getPersistentData().putDouble(ARMOR_TAG, armor);
            setAttributeBase(transformed, Attributes.ARMOR, armor, true);
        }
        if (!Double.isNaN(toughness)) {
            transformed.getPersistentData().putDouble(ARMOR_TOUGHNESS_TAG, toughness);
            setAttributeBase(transformed, Attributes.ARMOR_TOUGHNESS, toughness, false);
        }
        if (!Double.isNaN(protection)) {
            transformed.getPersistentData().putDouble(PROTECTION_TAG, protection);
        }
        if (!Double.isNaN(movementSpeed)) {
            transformed.getPersistentData().putDouble(MOVEMENT_SPEED_TAG, movementSpeed);
            setAttributeBase(transformed, Attributes.MOVEMENT_SPEED, movementSpeed, false);
        }

        removeMobEffects(transformed, source, MOB_EFFECTS_TAG);
        removeMobEffects(transformed, source, TF_MOB_EFFECTS_TAG);
        if (source.contains(TF_MOB_EFFECTS_TAG, 9)) {
            transformed.getPersistentData().put(MOB_EFFECTS_TAG, source.getList(TF_MOB_EFFECTS_TAG, 10).copy());
        }
        applyMobEffects(transformed);

        int transformedStage = Math.min(3, source.getInt(TRANSFORM_STAGE_TAG) + 1);
        transformed.getPersistentData().putInt(TRANSFORM_STAGE_TAG, transformedStage);
        activateStage(transformed.getPersistentData(), transformedStage + 1);
        boolean nextTransformationAllowed = transformedStage < 2
                ? !source.contains(CAN_TRANSFORM_2_TAG) || source.getBoolean(CAN_TRANSFORM_2_TAG)
                : !source.contains(CAN_TRANSFORM_3_TAG) || source.getBoolean(CAN_TRANSFORM_3_TAG);
        if (!nextTransformationAllowed) {
            if (transformed instanceof DBSagasEntity sagaEntity) {
                sagaEntity.setTransformationDisabled(true);
            }
            transformed.getPersistentData().putBoolean(QUEST_NO_TRANSFORM_TAG, true);
        }
    }

    public static void copyRevampQuestTags(LivingEntity source, LivingEntity target) {
        CompoundTag sourceTag = source.getPersistentData();
        CompoundTag targetTag = target.getPersistentData();
        for (String key : new String[]{
                ARMOR_TAG, ARMOR_TOUGHNESS_TAG, PROTECTION_TAG, ARMOR_CONFIGURED_TAG,
                MOVEMENT_SPEED_TAG,
                TF_ARMOR_TAG, TF_ARMOR_TOUGHNESS_TAG, TF_PROTECTION_TAG, TF_MOVEMENT_SPEED_TAG,
                TF_ARMOR_MULT_TAG, TF_ARMOR_TOUGHNESS_MULT_TAG, TF_PROTECTION_MULT_TAG, TF_MOVEMENT_SPEED_MULT_TAG,
                MOB_EFFECTS_TAG, TF_MOB_EFFECTS_TAG, TRANSFORM_STAGE_TAG,
                CAN_TRANSFORM_2_TAG, CAN_TRANSFORM_3_TAG, VERIFIED_QUEST_SPAWN_TAG
        }) {
            if (sourceTag.contains(key)) {
                targetTag.put(key, sourceTag.get(key).copy());
            }
        }
        for (String key : sourceTag.getAllKeys()) {
            if (key.startsWith("dmzrevamp_transform2_") || key.startsWith("dmzrevamp_transform3_")) {
                targetTag.put(key, sourceTag.get(key).copy());
            }
        }
    }

    private static void activateStage(CompoundTag tag, int stage) {
        String[][] mappings = {
                {DMZ_TF_HP_ABS, "hp_abs"}, {DMZ_TF_HP_MULT, "hp_mult"},
                {DMZ_TF_MELEE_ABS, "melee_abs"}, {DMZ_TF_MELEE_MULT, "melee_mult"},
                {DMZ_TF_KI_ABS, "ki_abs"}, {DMZ_TF_KI_MULT, "ki_mult"},
                {TF_ARMOR_TAG, "armor_abs"}, {TF_ARMOR_MULT_TAG, "armor_mult"},
                {TF_ARMOR_TOUGHNESS_TAG, "toughness_abs"}, {TF_ARMOR_TOUGHNESS_MULT_TAG, "toughness_mult"},
                {TF_PROTECTION_TAG, "protection_abs"}, {TF_PROTECTION_MULT_TAG, "protection_mult"},
                {TF_MOVEMENT_SPEED_TAG, "speed_abs"}, {TF_MOVEMENT_SPEED_MULT_TAG, "speed_mult"},
                {DMZ_TF_TRIGGER, "trigger"}
        };
        for (String[] mapping : mappings) {
            tag.remove(mapping[0]);
            if (stage <= 3) {
                String stored = TransformStageOverridesWriter.key(stage, mapping[1]);
                if (tag.contains(stored)) tag.putDouble(mapping[0], tag.getDouble(stored));
            }
        }
        tag.remove(TF_MOB_EFFECTS_TAG);
        if (stage <= 3) {
            String effects = TransformStageOverridesWriter.key(stage, "effects");
            if (tag.contains(effects, 9)) tag.put(TF_MOB_EFFECTS_TAG, tag.getList(effects, 10).copy());
        }
    }

    public static double protectionValue(LivingEntity entity) {
        return entity.getPersistentData().contains(PROTECTION_TAG) ? entity.getPersistentData().getDouble(PROTECTION_TAG) : 0D;
    }

    public static double questKiDamageValue(LivingEntity entity) {
        KillObjective objective = killObjective(entity);
        if (objective == null) {
            return 0D;
        }
        double value = objective.getKiDamage();
        return Double.isFinite(value) && value > 0D ? value : 0D;
    }

    private static KillObjective killObjective(Entity entity) {
        if (!hasDmzQuestSpawnTags(entity)) {
            return null;
        }
        Quest quest = QuestRegistry.getQuest(entity.getPersistentData().getString(QUEST_KEY_TAG));
        int objectiveIndex = entity.getPersistentData().getInt(QUEST_OBJECTIVE_INDEX_TAG);
        if (quest == null || objectiveIndex < 0 || objectiveIndex >= quest.getObjectives().size()) {
            return null;
        }
        QuestObjective objective = quest.getObjectives().get(objectiveIndex);
        return objective instanceof KillObjective killObjective ? killObjective : null;
    }

    private static void saveMobEffects(CompoundTag tag, List<QuestMobEffectConfig> effects) {
        saveMobEffects(tag, MOB_EFFECTS_TAG, effects);
    }

    public static void saveMobEffects(CompoundTag tag, String key, List<QuestMobEffectConfig> effects) {
        if (effects == null || effects.isEmpty()) {
            return;
        }
        ListTag list = new ListTag();
        for (QuestMobEffectConfig effect : effects) {
            list.add(effect.save());
        }
        tag.put(key, list);
    }

    public static void applyMobEffects(LivingEntity entity) {
        CompoundTag tag = entity.getPersistentData();
        if (!tag.contains(MOB_EFFECTS_TAG, 9)) {
            return;
        }
        ListTag effects = tag.getList(MOB_EFFECTS_TAG, 10);
        for (int i = 0; i < effects.size(); i++) {
            QuestMobEffectConfig config = QuestMobEffectConfig.load(effects.getCompound(i));
            MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(config.effectId()));
            if (effect != null) {
                entity.addEffect(new MobEffectInstance(effect, config.durationTicks(), config.amplifier(), config.ambient(), config.visible(), config.showIcon()));
            }
        }
    }

    private static void removeMobEffects(LivingEntity entity, CompoundTag source, String key) {
        if (!source.contains(key, 9)) {
            return;
        }
        ListTag effects = source.getList(key, 10);
        for (int i = 0; i < effects.size(); i++) {
            QuestMobEffectConfig config = QuestMobEffectConfig.load(effects.getCompound(i));
            MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(config.effectId()));
            if (effect != null) {
                entity.removeEffect(effect);
            }
        }
    }

    private static double resolveTransformValue(CompoundTag tag, String exactKey, String multiplierKey, double baseValue) {
        if (tag.contains(exactKey)) {
            return tag.getDouble(exactKey);
        }
        if (tag.contains(multiplierKey)) {
            return baseValue * tag.getDouble(multiplierKey);
        }
        return Double.NaN;
    }

    private static void putNullable(CompoundTag tag, String key, Double value) {
        if (value != null) {
            tag.putDouble(key, value);
        }
    }

    private static void setAttributeBase(LivingEntity entity, net.minecraft.world.entity.ai.attributes.Attribute attribute, double value, boolean removeBaseArmorModifier) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        if (removeBaseArmorModifier && instance.getModifier(BASE_MOB_ARMOR_UUID) != null) {
            instance.removeModifier(BASE_MOB_ARMOR_UUID);
        }
        instance.setBaseValue(Math.max(0D, value));
    }

    private static double attributeValue(LivingEntity entity, net.minecraft.world.entity.ai.attributes.Attribute attribute) {
        AttributeInstance instance = entity.getAttribute(attribute);
        return instance == null ? 0D : instance.getValue();
    }
}
