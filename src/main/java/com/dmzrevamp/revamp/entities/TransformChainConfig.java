package com.dmzrevamp.revamp.entities;

import com.dmzrevamp.revamp.quest.TransformStageOverrides;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TransformChainConfig {
    private static JsonObject loadedRoot = new JsonObject();

    private TransformChainConfig() {
    }

    public static synchronized TransformStageOverrides get(String entityId, int stage) {
        JsonObject defaults = object(loadedRoot, "transformDefaults");
        JsonObject stats = object(object(loadedRoot, "defaultEntityStats"), entityId);
        return merge(TransformStageOverrides.parse(stats, stage), TransformStageOverrides.parse(defaults, stage));
    }

    public static synchronized void reload() {
        Path path = FMLPaths.CONFIGDIR.get().resolve("dragonminez").resolve("entities.json");
        loadedRoot = new JsonObject();
        if (!Files.isRegularFile(path)) return;
        try (Reader reader = Files.newBufferedReader(path)) {
            loadedRoot = JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception ignored) {
            loadedRoot = new JsonObject();
        }
    }

    private static JsonObject object(JsonObject parent, String key) {
        return parent != null && parent.has(key) && parent.get(key).isJsonObject()
                ? parent.getAsJsonObject(key) : new JsonObject();
    }

    private static TransformStageOverrides merge(TransformStageOverrides value, TransformStageOverrides fallback) {
        ValuePair health = pair(value.health(), value.healthMulti(), fallback.health(), fallback.healthMulti());
        ValuePair melee = pair(value.meleeDamage(), value.meleeDamageMulti(), fallback.meleeDamage(), fallback.meleeDamageMulti());
        ValuePair ki = pair(value.kiDamage(), value.kiDamageMulti(), fallback.kiDamage(), fallback.kiDamageMulti());
        ValuePair armor = pair(value.armor(), value.armorMulti(), fallback.armor(), fallback.armorMulti());
        ValuePair toughness = pair(value.armorToughness(), value.armorToughnessMulti(), fallback.armorToughness(), fallback.armorToughnessMulti());
        ValuePair protection = pair(value.protection(), value.protectionMulti(), fallback.protection(), fallback.protectionMulti());
        ValuePair speed = pair(value.movementSpeed(), value.movementSpeedMulti(), fallback.movementSpeed(), fallback.movementSpeedMulti());
        return new TransformStageOverrides(
                health.exact(), health.multiplier(),
                melee.exact(), melee.multiplier(),
                ki.exact(), ki.multiplier(),
                armor.exact(), armor.multiplier(),
                toughness.exact(), toughness.multiplier(),
                protection.exact(), protection.multiplier(),
                speed.exact(), speed.multiplier(),
                first(value.triggerPercent(), fallback.triggerPercent()),
                value.mobEffects() != null && !value.mobEffects().isEmpty() ? value.mobEffects() : fallback.mobEffects()
        );
    }

    private static ValuePair pair(Double exact, Double multiplier, Double fallbackExact, Double fallbackMultiplier) {
        return exact != null || multiplier != null
                ? new ValuePair(exact, multiplier)
                : new ValuePair(fallbackExact, fallbackMultiplier);
    }

    private static Double first(Double value, Double fallback) {
        return value != null ? value : fallback;
    }

    private record ValuePair(Double exact, Double multiplier) {
    }
}
