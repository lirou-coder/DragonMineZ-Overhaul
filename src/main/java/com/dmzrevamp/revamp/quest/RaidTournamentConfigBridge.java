package com.dmzrevamp.revamp.quest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Reads the Overhaul's flat quest-style stat keys from DMZ raid/tournament files. */
public final class RaidTournamentConfigBridge {
    private RaidTournamentConfigBridge() {}

    public static void populate(JsonObject json, Object target) {
        if (json == null || !(target instanceof RevampKillObjectiveData data)) return;
        data.dmzrevamp$setDefense(number(json, "Defense", "defense"));
        data.dmzrevamp$setArmor(number(json, "Armor", "armor"));
        data.dmzrevamp$setArmorToughness(number(json, "ArmorToughness", "armorToughness"));
        data.dmzrevamp$setProtection(number(json, "Protection", "protection"));
        data.dmzrevamp$setMovementSpeed(number(json, "movementSpeed", "MovementSpeed"));

        data.dmzrevamp$setTransformHealth(number(json, "TransformHealth", "transformHealth"));
        data.dmzrevamp$setTransformHealthMultiplier(number(json,
                "TransformHealthMultiplier", "TransformHealthMulti"));
        data.dmzrevamp$setTransformMeleeDamage(number(json, "TransformMeleeDamage", "transformMeleeDamage"));
        data.dmzrevamp$setTransformMeleeDamageMultiplier(number(json,
                "TransformMeleeDamageMultiplier", "TransformMeleeDamageMulti", "TransformMeleeMulti"));
        data.dmzrevamp$setTransformKiDamage(number(json, "TransformKiDamage", "transformKiDamage"));
        data.dmzrevamp$setTransformKiDamageMultiplier(number(json,
                "TransformKiDamageMultiplier", "TransformKiDamageMulti", "TransformKiMulti"));
        data.dmzrevamp$setTransformTriggerPercent(number(json, "TransformTriggerPercent", "TransformTriggerHealthPercent"));
        data.dmzrevamp$setTransformArmor(number(json, "TransformArmor"));
        data.dmzrevamp$setTransformDefense(number(json, "TransformDefense", "transformDefense"));
        data.dmzrevamp$setTransformArmorToughness(number(json, "TransformArmorToughness"));
        data.dmzrevamp$setTransformProtection(number(json, "TransformProtection"));
        data.dmzrevamp$setTransformMovementSpeed(number(json, "TransformMovementSpeed", "TransformmovementSpeed"));
        data.dmzrevamp$setTransformArmorMultiplier(number(json, "TransformArmorMultiplier", "TransformArmorMulti"));
        data.dmzrevamp$setTransformDefenseMultiplier(number(json, "TransformDefenseMulti", "TransformDefenseMultiplier"));
        data.dmzrevamp$setTransformArmorToughnessMultiplier(number(json,
                "TransformArmorToughnessMultiplier", "TransformArmorToughnessMulti"));
        data.dmzrevamp$setTransformProtectionMultiplier(number(json,
                "TransformProtectionMultiplier", "TransformProtectionMulti"));
        data.dmzrevamp$setTransformMovementSpeedMultiplier(number(json,
                "TransformMovementSpeedMultiplier", "TransformMovementSpeedMulti"));
        data.dmzrevamp$setMobEffects(QuestMobEffectConfig.parseList(first(json, "mobEffects", "mobEffect")));
        data.dmzrevamp$setTransformMobEffects(QuestMobEffectConfig.parseList(
                first(json, "TransformMobEffects", "TransformMobEffect")));

        for (int stage = 2; stage <= 4; stage++) {
            data.dmzrevamp$setTransformStage(stage, TransformStageOverrides.parse(json, stage));
            data.dmzrevamp$setCanTransformStage(stage, bool(json, "canTransform" + stage, true));
        }
        for (int stage = 1; stage <= 4; stage++) {
            String prefix = stage == 1 ? "Transform" : "Transform" + stage;
            data.dmzrevamp$setTransformEntity(stage,
                    string(json, prefix + "Entity", prefix + "Override", prefix + "Overide"));
        }
    }

    private static JsonElement first(JsonObject json, String... keys) {
        for (String key : keys) {
            JsonElement value = json.get(key);
            if (value != null && !value.isJsonNull()) return value;
        }
        for (var entry : json.entrySet()) {
            for (String key : keys) {
                if (entry.getKey().equalsIgnoreCase(key) && !entry.getValue().isJsonNull()) return entry.getValue();
            }
        }
        return null;
    }

    private static Double number(JsonObject json, String... keys) {
        JsonElement value = first(json, keys);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()
                ? value.getAsDouble() : null;
    }

    private static boolean bool(JsonObject json, String key, boolean fallback) {
        JsonElement value = first(json, key);
        return value != null && value.isJsonPrimitive() ? value.getAsBoolean() : fallback;
    }

    private static String string(JsonObject json, String... keys) {
        JsonElement value = first(json, keys);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                ? value.getAsString() : null;
    }
}
