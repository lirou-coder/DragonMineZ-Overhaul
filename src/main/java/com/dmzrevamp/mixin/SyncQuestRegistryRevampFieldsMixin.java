package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.quest.QuestMobEffectConfig;
import com.dmzrevamp.revamp.quest.RevampKillObjectiveData;
import com.dmzrevamp.revamp.quest.TransformStageOverrides;
import com.dragonminez.common.network.S2C.SyncQuestRegistryS2C;
import com.dragonminez.common.quest.QuestObjective;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** Includes Overhaul-only objective fields in DMZ's manually serialized quest sync. */
@Mixin(value = SyncQuestRegistryS2C.class, remap = false)
public abstract class SyncQuestRegistryRevampFieldsMixin {
    @Inject(method = "serializeObjective", at = @At("RETURN"), cancellable = true)
    private static void dmzrevamp$serializeObjectiveFields(QuestObjective objective,
                                                           CallbackInfoReturnable<JsonObject> cir) {
        if (!(objective instanceof RevampKillObjectiveData data)) return;

        JsonObject json = cir.getReturnValue();
        add(json, "Defense", data.dmzrevamp$getDefense());
        add(json, "Armor", data.dmzrevamp$getArmor());
        add(json, "ArmorToughness", data.dmzrevamp$getArmorToughness());
        add(json, "Protection", data.dmzrevamp$getProtection());
        add(json, "movementSpeed", data.dmzrevamp$getMovementSpeed());
        add(json, "TransformArmor", data.dmzrevamp$getTransformArmor());
        add(json, "TransformDefense", data.dmzrevamp$getTransformDefense());
        add(json, "TransformArmorToughness", data.dmzrevamp$getTransformArmorToughness());
        add(json, "TransformProtection", data.dmzrevamp$getTransformProtection());
        add(json, "TransformMovementSpeed", data.dmzrevamp$getTransformMovementSpeed());
        add(json, "TransformArmorMultiplier", data.dmzrevamp$getTransformArmorMultiplier());
        add(json, "TransformDefenseMulti", data.dmzrevamp$getTransformDefenseMultiplier());
        add(json, "TransformArmorToughnessMultiplier", data.dmzrevamp$getTransformArmorToughnessMultiplier());
        add(json, "TransformProtectionMultiplier", data.dmzrevamp$getTransformProtectionMultiplier());
        add(json, "TransformMovementSpeedMultiplier", data.dmzrevamp$getTransformMovementSpeedMultiplier());
        addEffects(json, "mobEffects", data.dmzrevamp$getMobEffects());
        addEffects(json, "TransformMobEffects", data.dmzrevamp$getTransformMobEffects());

        for (int stage = 2; stage <= 4; stage++) {
            String prefix = "Transform" + stage;
            TransformStageOverrides values = data.dmzrevamp$getTransformStage(stage);
            if (values != null) {
                add(json, prefix + "Health", values.health());
                add(json, prefix + "HealthMulti", values.healthMulti());
                add(json, prefix + "MeleeDamage", values.meleeDamage());
                add(json, prefix + "MeleeDamageMulti", values.meleeDamageMulti());
                add(json, prefix + "KiDamage", values.kiDamage());
                add(json, prefix + "KiDamageMulti", values.kiDamageMulti());
                add(json, prefix + "Defense", values.defense());
                add(json, prefix + "DefenseMulti", values.defenseMulti());
                add(json, prefix + "Armor", values.armor());
                add(json, prefix + "ArmorMulti", values.armorMulti());
                add(json, prefix + "ArmorToughness", values.armorToughness());
                add(json, prefix + "ArmorToughnessMulti", values.armorToughnessMulti());
                add(json, prefix + "Protection", values.protection());
                add(json, prefix + "ProtectionMulti", values.protectionMulti());
                add(json, prefix + "MovementSpeed", values.movementSpeed());
                add(json, prefix + "MovementSpeedMulti", values.movementSpeedMulti());
                add(json, prefix + "TriggerPercent", values.triggerPercent());
                addEffects(json, prefix + "MobEffects", values.mobEffects());
            }
            json.addProperty("canTransform" + stage, data.dmzrevamp$canTransformStage(stage));
            String entityId = data.dmzrevamp$getTransformEntity(stage);
            if (entityId != null && !entityId.isBlank()) json.addProperty(prefix + "Entity", entityId);
        }

        String firstStageEntity = data.dmzrevamp$getTransformEntity(1);
        if (firstStageEntity != null && !firstStageEntity.isBlank()) json.addProperty("TransformEntity", firstStageEntity);
        cir.setReturnValue(json);
    }

    private static void add(JsonObject json, String key, Double value) {
        if (value != null && Double.isFinite(value)) json.addProperty(key, value);
    }

    private static void addEffects(JsonObject json, String key, List<QuestMobEffectConfig> effects) {
        if (effects == null || effects.isEmpty()) return;
        JsonArray array = new JsonArray();
        for (QuestMobEffectConfig effect : effects) {
            JsonObject entry = new JsonObject();
            entry.addProperty("effectId", effect.effectId());
            entry.addProperty("amplifier", effect.amplifier());
            entry.addProperty("durationTicks", effect.durationTicks());
            entry.addProperty("ambient", effect.ambient());
            entry.addProperty("visible", effect.visible());
            entry.addProperty("showIcon", effect.showIcon());
            array.add(entry);
        }
        json.add(key, array);
    }
}
