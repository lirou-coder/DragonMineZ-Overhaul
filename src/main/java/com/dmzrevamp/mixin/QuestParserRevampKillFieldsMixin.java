package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.quest.QuestMobEffectConfig;
import com.dmzrevamp.revamp.quest.RevampKillObjectiveData;
import com.dmzrevamp.revamp.quest.TransformStageOverrides;
import com.dragonminez.common.quest.QuestObjective;
import com.dragonminez.common.quest.objectives.KillObjective;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Mixin(targets = "com.dragonminez.common.quest.QuestParser", remap = false)
public abstract class QuestParserRevampKillFieldsMixin {
    private static final String[] DMZREVAMP_KILL_KEYS = {
            "Armor", "ArmorToughness", "Protection", "movementSpeed", "MovementSpeed",
            "TransformArmor", "TransformArmorToughness", "TransformProtection", "TransformMovementSpeed",
            "TransformArmorMultiplier", "TransformArmorToughnessMultiplier", "TransformProtectionMultiplier", "TransformMovementSpeedMultiplier",
            "mobEffects", "mobEffect", "TransformMobEffects", "TransformMobEffect",
            "canTransform2", "canTransform3"
    };
    private static final String[] DMZREVAMP_CHAIN_SUFFIXES = {
            "Health", "HealthMulti", "HealthMultiplier",
            "MeleeDamage", "MeleeDamageMulti", "MeleeDamageMultiplier", "MeleeMulti",
            "KiDamage", "KiDamageMulti", "KiDamageMultiplier", "KiMulti",
            "Armor", "ArmorMulti", "ArmorMultiplier",
            "ArmorToughness", "ArmorToughnessMulti", "ArmorToughnessMultiplier",
            "Protection", "ProtectionMulti", "ProtectionMultiplier",
            "MovementSpeed", "movementSpeed", "MovementSpeedMulti", "MovementSpeedMultiplier",
            "TriggerPercent", "TriggerHealthPercent", "MobEffects", "MobEffect"
    };
    private static final ThreadLocal<ArrayDeque<Map<String, JsonElement>>> DMZREVAMP_REMOVED_KEYS =
            ThreadLocal.withInitial(ArrayDeque::new);

    @Inject(method = "parseObjective", at = @At("RETURN"))
    private static void dmzrevamp$parseKillExtraFields(JsonObject object, CallbackInfoReturnable<QuestObjective> cir) {
        if (!(cir.getReturnValue() instanceof KillObjective objective) || !(objective instanceof RevampKillObjectiveData data)) {
            return;
        }

        data.dmzrevamp$setArmor(nullableDouble(object, "Armor"));
        data.dmzrevamp$setArmorToughness(nullableDouble(object, "ArmorToughness"));
        data.dmzrevamp$setProtection(nullableDouble(object, "Protection"));
        data.dmzrevamp$setMovementSpeed(nullableDouble(object, "movementSpeed", "MovementSpeed"));
        data.dmzrevamp$setTransformArmor(nullableDouble(object, "TransformArmor"));
        data.dmzrevamp$setTransformArmorToughness(nullableDouble(object, "TransformArmorToughness"));
        data.dmzrevamp$setTransformProtection(nullableDouble(object, "TransformProtection"));
        data.dmzrevamp$setTransformMovementSpeed(nullableDouble(object, "TransformMovementSpeed"));
        data.dmzrevamp$setTransformArmorMultiplier(nullableDouble(object, "TransformArmorMultiplier"));
        data.dmzrevamp$setTransformArmorToughnessMultiplier(nullableDouble(object, "TransformArmorToughnessMultiplier"));
        data.dmzrevamp$setTransformProtectionMultiplier(nullableDouble(object, "TransformProtectionMultiplier"));
        data.dmzrevamp$setTransformMovementSpeedMultiplier(nullableDouble(object, "TransformMovementSpeedMultiplier"));
        data.dmzrevamp$setMobEffects(parseMobEffects(object));
        data.dmzrevamp$setTransformMobEffects(parseMobEffects(object, "TransformMobEffects", "TransformMobEffect"));
        data.dmzrevamp$setTransformStage(2, TransformStageOverrides.parse(object, 2));
        data.dmzrevamp$setTransformStage(3, TransformStageOverrides.parse(object, 3));
        data.dmzrevamp$setCanTransformStage(2, nullableBoolean(object, "canTransform2", true));
        data.dmzrevamp$setCanTransformStage(3, nullableBoolean(object, "canTransform3", true));
    }

    @Inject(method = "validateObjective", at = @At("HEAD"))
    private static void dmzrevamp$hideExtraKillKeysFromValidation(String namespace, String path, String label, JsonObject object, CallbackInfo ci) {
        Map<String, JsonElement> removed = new LinkedHashMap<>();
        if (isKillObjective(object)) {
            for (String key : DMZREVAMP_KILL_KEYS) {
                if (object.has(key)) {
                    removed.put(key, object.remove(key));
                }
            }
            for (int stage = 2; stage <= 3; stage++) {
                for (String suffix : DMZREVAMP_CHAIN_SUFFIXES) {
                    String key = "Transform" + stage + suffix;
                    if (object.has(key)) removed.put(key, object.remove(key));
                }
            }
        }
        DMZREVAMP_REMOVED_KEYS.get().push(removed);
    }

    @Inject(method = "validateObjective", at = @At("RETURN"))
    private static void dmzrevamp$restoreExtraKillKeysAfterValidation(String namespace, String path, String label, JsonObject object, CallbackInfo ci) {
        ArrayDeque<Map<String, JsonElement>> stack = DMZREVAMP_REMOVED_KEYS.get();
        if (stack.isEmpty()) {
            return;
        }
        Map<String, JsonElement> removed = stack.pop();
        for (Map.Entry<String, JsonElement> entry : removed.entrySet()) {
            object.add(entry.getKey(), entry.getValue());
        }
    }

    private static boolean isKillObjective(JsonObject object) {
        if (object == null || !object.has("type") || object.get("type").isJsonNull()) {
            return false;
        }
        return "KILL".equalsIgnoreCase(object.get("type").getAsString());
    }

    private static Double nullableDouble(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return null;
        }
        return object.get(key).getAsDouble();
    }

    private static Double nullableDouble(JsonObject object, String primaryKey, String fallbackKey) {
        Double primary = nullableDouble(object, primaryKey);
        return primary != null ? primary : nullableDouble(object, fallbackKey);
    }

    private static boolean nullableBoolean(JsonObject object, String key, boolean fallback) {
        return object != null && object.has(key) && !object.get(key).isJsonNull()
                ? object.get(key).getAsBoolean() : fallback;
    }

    private static List<QuestMobEffectConfig> parseMobEffects(JsonObject object) {
        return parseMobEffects(object, "mobEffects", "mobEffect");
    }

    private static List<QuestMobEffectConfig> parseMobEffects(JsonObject object, String arrayKey, String singleKey) {
        JsonElement element = object.has(arrayKey) ? object.get(arrayKey) : object.get(singleKey);
        if (element == null || element.isJsonNull()) {
            return List.of();
        }
        List<QuestMobEffectConfig> effects = new ArrayList<>();
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement entry : array) {
                addEffect(effects, entry);
            }
        } else {
            addEffect(effects, element);
        }
        return effects;
    }

    private static void addEffect(List<QuestMobEffectConfig> effects, JsonElement element) {
        if (element != null && element.isJsonObject()) {
            QuestMobEffectConfig config = QuestMobEffectConfig.fromJson(element.getAsJsonObject());
            if (config != null) {
                effects.add(config);
            }
        }
    }
}
