package com.dmzrevamp.revamp.quest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.List;

public record TransformStageOverrides(
        Double health, Double healthMulti,
        Double meleeDamage, Double meleeDamageMulti,
        Double kiDamage, Double kiDamageMulti,
        Double armor, Double armorMulti,
        Double armorToughness, Double armorToughnessMulti,
        Double protection, Double protectionMulti,
        Double movementSpeed, Double movementSpeedMulti,
        Double triggerPercent,
        List<QuestMobEffectConfig> mobEffects
) {
    public static final TransformStageOverrides EMPTY = new TransformStageOverrides(
            null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, List.of()
    );

    public static TransformStageOverrides parse(JsonObject object, int stage) {
        if (object == null || stage < 2 || stage > 3) {
            return EMPTY;
        }
        String prefix = "Transform" + stage;
        return new TransformStageOverrides(
                number(object, prefix + "Health"),
                number(object, prefix + "HealthMulti", prefix + "HealthMultiplier"),
                number(object, prefix + "MeleeDamage"),
                number(object, prefix + "MeleeDamageMulti", prefix + "MeleeDamageMultiplier", prefix + "MeleeMulti"),
                number(object, prefix + "KiDamage"),
                number(object, prefix + "KiDamageMulti", prefix + "KiDamageMultiplier", prefix + "KiMulti"),
                number(object, prefix + "Armor"),
                number(object, prefix + "ArmorMulti", prefix + "ArmorMultiplier"),
                number(object, prefix + "ArmorToughness"),
                number(object, prefix + "ArmorToughnessMulti", prefix + "ArmorToughnessMultiplier"),
                number(object, prefix + "Protection"),
                number(object, prefix + "ProtectionMulti", prefix + "ProtectionMultiplier"),
                number(object, prefix + "MovementSpeed", prefix + "movementSpeed"),
                number(object, prefix + "MovementSpeedMulti", prefix + "MovementSpeedMultiplier"),
                number(object, prefix + "TriggerPercent", prefix + "TriggerHealthPercent"),
                QuestMobEffectConfig.parseList(first(object, prefix + "MobEffects", prefix + "MobEffect"))
        );
    }

    public boolean isEmpty() {
        return this.equals(EMPTY);
    }

    private static Double number(JsonObject object, String... keys) {
        JsonElement element = first(object, keys);
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()
                ? element.getAsDouble() : null;
    }

    private static JsonElement first(JsonObject object, String... keys) {
        for (String key : keys) {
            if (object.has(key) && !object.get(key).isJsonNull()) {
                return object.get(key);
            }
        }
        return null;
    }
}
