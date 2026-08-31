package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.entities.RevampEntityStatsData;
import com.dmzrevamp.revamp.entities.RevampTransformSettingsData;
import com.dmzrevamp.revamp.quest.QuestMobEffectConfig;
import com.dragonminez.common.config.EntitiesConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Mixin(EntitiesConfig.class)
public abstract class EntitiesConfigDefaultsMixin {
    private static final String SNAPSHOT = "data/dmzrevamp/defaults/config/entities.json";

    @Shadow(remap = false)
    private Map<String, EntitiesConfig.EntityStats> defaultEntityStats;
    @Shadow(remap = false)
    private EntitiesConfig.TransformSettings transformDefaults;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void dmzrevamp$setEntityDefaults(CallbackInfo ci) {
        try (InputStream stream = EntitiesConfigDefaultsMixin.class.getClassLoader().getResourceAsStream(SNAPSHOT)) {
            if (stream == null) {
                return;
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                JsonObject statsRoot = root.getAsJsonObject("defaultEntityStats");
                if (statsRoot != null) {
                    Map<String, EntitiesConfig.EntityStats> stats = new LinkedHashMap<>();
                    for (Map.Entry<String, JsonElement> entry : statsRoot.entrySet()) {
                        if (!entry.getValue().isJsonObject()) {
                            continue;
                        }
                        JsonObject object = entry.getValue().getAsJsonObject();
                        EntitiesConfig.EntityStats entityStats = new EntitiesConfig.EntityStats();
                        setIfPresent(object, "health", entityStats::setHealth);
                        setIfPresent(object, "meleeDamage", entityStats::setMeleeDamage);
                        setIfPresent(object, "kiDamage", entityStats::setKiDamage);
                        if (entityStats instanceof RevampEntityStatsData data) {
                            setEntityExtraFields(object, data);
                        }
                        stats.put(entry.getKey(), entityStats);
                    }
                    this.defaultEntityStats = stats;
                }

                JsonObject transformRoot = root.getAsJsonObject("transformDefaults");
                if (transformRoot != null) {
                    EntitiesConfig.TransformSettings settings = new EntitiesConfig.TransformSettings();
                    setIfPresent(transformRoot, "healthMultiplier", settings::setHealthMultiplier);
                    setIfPresent(transformRoot, "meleeMultiplier", settings::setMeleeMultiplier);
                    setIfPresent(transformRoot, "kiMultiplier", settings::setKiMultiplier);
                    setIfPresent(transformRoot, "triggerHealthPercent", settings::setTriggerHealthPercent);
                    if (settings instanceof RevampTransformSettingsData data) {
                        setTransformExtraFields(transformRoot, data);
                    }
                    this.transformDefaults = settings;
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void setIfPresent(JsonObject object, String key, java.util.function.Consumer<Double> setter) {
        JsonElement element = object.get(key);
        if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            setter.accept(element.getAsDouble());
        }
    }

    private static void setEntityExtraFields(JsonObject object, RevampEntityStatsData data) {
        data.dmzrevamp$setArmor(nullableDouble(object, "Armor", "armor"));
        data.dmzrevamp$setArmorToughness(nullableDouble(object, "ArmorToughness", "armorToughness"));
        data.dmzrevamp$setProtection(nullableDouble(object, "Protection", "protection"));
        data.dmzrevamp$setMovementSpeed(nullableDouble(object, "movementSpeed", "MovementSpeed"));
        data.dmzrevamp$setTransformArmor(nullableDouble(object, "TransformArmor", "transformArmor"));
        data.dmzrevamp$setTransformArmorToughness(nullableDouble(object, "TransformArmorToughness", "transformArmorToughness"));
        data.dmzrevamp$setTransformProtection(nullableDouble(object, "TransformProtection", "transformProtection"));
        data.dmzrevamp$setTransformMovementSpeed(nullableDouble(object, "TransformMovementSpeed", "transformMovementSpeed"));
        data.dmzrevamp$setTransformArmorMultiplier(nullableDouble(object, "TransformArmorMultiplier", "transformArmorMultiplier"));
        data.dmzrevamp$setTransformArmorToughnessMultiplier(nullableDouble(object, "TransformArmorToughnessMultiplier", "transformArmorToughnessMultiplier"));
        data.dmzrevamp$setTransformProtectionMultiplier(nullableDouble(object, "TransformProtectionMultiplier", "transformProtectionMultiplier"));
        data.dmzrevamp$setTransformMovementSpeedMultiplier(nullableDouble(object, "TransformMovementSpeedMultiplier", "transformMovementSpeedMultiplier"));
        data.dmzrevamp$setMobEffects(parseMobEffects(object, "mobEffects", "mobEffect", "MobEffects", "MobEffect"));
        data.dmzrevamp$setTransformMobEffects(parseMobEffects(object, "TransformMobEffects", "TransformMobEffect", "transformMobEffects", "transformMobEffect"));
    }

    private static void setTransformExtraFields(JsonObject object, RevampTransformSettingsData data) {
        data.dmzrevamp$setTransformArmor(nullableDouble(object, "TransformArmor", "transformArmor"));
        data.dmzrevamp$setTransformArmorToughness(nullableDouble(object, "TransformArmorToughness", "transformArmorToughness"));
        data.dmzrevamp$setTransformProtection(nullableDouble(object, "TransformProtection", "transformProtection"));
        data.dmzrevamp$setTransformMovementSpeed(nullableDouble(object, "TransformMovementSpeed", "transformMovementSpeed"));
        data.dmzrevamp$setTransformArmorMultiplier(nullableDouble(object, "TransformArmorMultiplier", "transformArmorMultiplier"));
        data.dmzrevamp$setTransformArmorToughnessMultiplier(nullableDouble(object, "TransformArmorToughnessMultiplier", "transformArmorToughnessMultiplier"));
        data.dmzrevamp$setTransformProtectionMultiplier(nullableDouble(object, "TransformProtectionMultiplier", "transformProtectionMultiplier"));
        data.dmzrevamp$setTransformMovementSpeedMultiplier(nullableDouble(object, "TransformMovementSpeedMultiplier", "transformMovementSpeedMultiplier"));
        data.dmzrevamp$setTransformMobEffects(parseMobEffects(object, "TransformMobEffects", "TransformMobEffect", "transformMobEffects", "transformMobEffect"));
    }

    private static Double nullableDouble(JsonObject object, String... keys) {
        for (String key : keys) {
            JsonElement element = object.get(key);
            if (element != null && !element.isJsonNull() && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
                return element.getAsDouble();
            }
        }
        return null;
    }

    private static List<QuestMobEffectConfig> parseMobEffects(JsonObject object, String... keys) {
        JsonElement element = null;
        for (String key : keys) {
            if (object.has(key)) {
                element = object.get(key);
                break;
            }
        }
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
            QuestMobEffectConfig effect = QuestMobEffectConfig.fromJson(element.getAsJsonObject());
            if (effect != null) {
                effects.add(effect);
            }
        }
    }
}
