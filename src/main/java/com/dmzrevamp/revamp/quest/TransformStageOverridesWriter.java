package com.dmzrevamp.revamp.quest;

import net.minecraft.nbt.CompoundTag;

public final class TransformStageOverridesWriter {
    private TransformStageOverridesWriter() {
    }

    public static String key(int stage, String suffix) {
        return "dmzrevamp_transform" + stage + "_" + suffix;
    }

    public static void save(CompoundTag tag, TransformStageOverrides values, int stage,
                            boolean replace, double healthScale, double damageScale) {
        if (values == null || values.isEmpty()) return;
        putPair(tag, key(stage, "hp_abs"), key(stage, "hp_mult"),
                scaled(values.health(), healthScale), values.healthMulti(), replace);
        putPair(tag, key(stage, "melee_abs"), key(stage, "melee_mult"),
                scaled(values.meleeDamage(), damageScale), values.meleeDamageMulti(), replace);
        putPair(tag, key(stage, "ki_abs"), key(stage, "ki_mult"),
                scaled(values.kiDamage(), damageScale), values.kiDamageMulti(), replace);
        putPair(tag, key(stage, "armor_abs"), key(stage, "armor_mult"),
                values.armor(), values.armorMulti(), replace);
        putPair(tag, key(stage, "toughness_abs"), key(stage, "toughness_mult"),
                values.armorToughness(), values.armorToughnessMulti(), replace);
        putPair(tag, key(stage, "protection_abs"), key(stage, "protection_mult"),
                values.protection(), values.protectionMulti(), replace);
        putPair(tag, key(stage, "speed_abs"), key(stage, "speed_mult"),
                values.movementSpeed(), values.movementSpeedMulti(), replace);
        put(tag, key(stage, "trigger"), values.triggerPercent(), replace);
        if (replace || !tag.contains(key(stage, "effects"))) {
            QuestSpawnAttributeApplier.saveMobEffects(tag, key(stage, "effects"), values.mobEffects());
        }
    }

    private static Double scaled(Double value, double scale) {
        return value == null ? null : value * scale;
    }

    private static void put(CompoundTag tag, String key, Double value, boolean replace) {
        if (value != null && (replace || !tag.contains(key))) tag.putDouble(key, value);
    }

    private static void putPair(CompoundTag tag, String exactKey, String multiplierKey,
                                Double exact, Double multiplier, boolean replace) {
        if (exact == null && multiplier == null) return;
        if (!replace && (tag.contains(exactKey) || tag.contains(multiplierKey))) return;
        if (exact != null) {
            tag.remove(multiplierKey);
            tag.putDouble(exactKey, exact);
        } else {
            tag.remove(exactKey);
            tag.putDouble(multiplierKey, multiplier);
        }
    }
}
