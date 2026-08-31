package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.entities.RevampTransformSettingsData;
import com.dmzrevamp.revamp.quest.QuestMobEffectConfig;
import com.dragonminez.common.config.EntitiesConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = EntitiesConfig.TransformSettings.class, remap = false)
public abstract class EntitiesConfigTransformSettingsRevampMixin implements RevampTransformSettingsData {
    @Unique
    @SerializedName(value = "TransformArmor", alternate = {"transformArmor"})
    private Double dmzrevamp$transformArmor;
    @Unique
    @SerializedName(value = "TransformArmorToughness", alternate = {"transformArmorToughness"})
    private Double dmzrevamp$transformArmorToughness;
    @Unique
    @SerializedName(value = "TransformProtection", alternate = {"transformProtection"})
    private Double dmzrevamp$transformProtection;
    @Unique
    @SerializedName(value = "TransformMovementSpeed", alternate = {"transformMovementSpeed"})
    private Double dmzrevamp$transformMovementSpeed;
    @Unique
    @SerializedName(value = "TransformArmorMultiplier", alternate = {"transformArmorMultiplier"})
    private Double dmzrevamp$transformArmorMultiplier;
    @Unique
    @SerializedName(value = "TransformArmorToughnessMultiplier", alternate = {"transformArmorToughnessMultiplier"})
    private Double dmzrevamp$transformArmorToughnessMultiplier;
    @Unique
    @SerializedName(value = "TransformProtectionMultiplier", alternate = {"transformProtectionMultiplier"})
    private Double dmzrevamp$transformProtectionMultiplier;
    @Unique
    @SerializedName(value = "TransformMovementSpeedMultiplier", alternate = {"transformMovementSpeedMultiplier"})
    private Double dmzrevamp$transformMovementSpeedMultiplier;
    @Unique
    private List<QuestMobEffectConfig> dmzrevamp$transformMobEffects = List.of();
    @Unique
    @SerializedName(value = "TransformMobEffects", alternate = {"transformMobEffects"})
    private JsonElement dmzrevamp$transformMobEffectsJson;
    @Unique
    @SerializedName(value = "TransformMobEffect", alternate = {"transformMobEffect"})
    private JsonElement dmzrevamp$transformMobEffectJson;

    @Override
    public Double dmzrevamp$getTransformArmor() {
        return dmzrevamp$transformArmor;
    }

    @Override
    public void dmzrevamp$setTransformArmor(Double value) {
        dmzrevamp$transformArmor = value;
    }

    @Override
    public Double dmzrevamp$getTransformArmorToughness() {
        return dmzrevamp$transformArmorToughness;
    }

    @Override
    public void dmzrevamp$setTransformArmorToughness(Double value) {
        dmzrevamp$transformArmorToughness = value;
    }

    @Override
    public Double dmzrevamp$getTransformProtection() {
        return dmzrevamp$transformProtection;
    }

    @Override
    public void dmzrevamp$setTransformProtection(Double value) {
        dmzrevamp$transformProtection = value;
    }

    @Override
    public Double dmzrevamp$getTransformMovementSpeed() {
        return dmzrevamp$transformMovementSpeed;
    }

    @Override
    public void dmzrevamp$setTransformMovementSpeed(Double value) {
        dmzrevamp$transformMovementSpeed = value;
    }

    @Override
    public Double dmzrevamp$getTransformArmorMultiplier() {
        return dmzrevamp$transformArmorMultiplier;
    }

    @Override
    public void dmzrevamp$setTransformArmorMultiplier(Double value) {
        dmzrevamp$transformArmorMultiplier = value;
    }

    @Override
    public Double dmzrevamp$getTransformArmorToughnessMultiplier() {
        return dmzrevamp$transformArmorToughnessMultiplier;
    }

    @Override
    public void dmzrevamp$setTransformArmorToughnessMultiplier(Double value) {
        dmzrevamp$transformArmorToughnessMultiplier = value;
    }

    @Override
    public Double dmzrevamp$getTransformProtectionMultiplier() {
        return dmzrevamp$transformProtectionMultiplier;
    }

    @Override
    public void dmzrevamp$setTransformProtectionMultiplier(Double value) {
        dmzrevamp$transformProtectionMultiplier = value;
    }

    @Override
    public Double dmzrevamp$getTransformMovementSpeedMultiplier() {
        return dmzrevamp$transformMovementSpeedMultiplier;
    }

    @Override
    public void dmzrevamp$setTransformMovementSpeedMultiplier(Double value) {
        dmzrevamp$transformMovementSpeedMultiplier = value;
    }

    @Override
    public List<QuestMobEffectConfig> dmzrevamp$getTransformMobEffects() {
        List<QuestMobEffectConfig> saved = dmzrevamp$transformMobEffects == null ? List.of() : dmzrevamp$transformMobEffects;
        return saved.isEmpty() ? dmzrevamp$parseMobEffects(dmzrevamp$transformMobEffectsJson, dmzrevamp$transformMobEffectJson) : saved;
    }

    @Override
    public void dmzrevamp$setTransformMobEffects(List<QuestMobEffectConfig> effects) {
        dmzrevamp$transformMobEffects = effects == null ? List.of() : List.copyOf(effects);
        dmzrevamp$transformMobEffectsJson = null;
        dmzrevamp$transformMobEffectJson = null;
    }

    @Unique
    private static List<QuestMobEffectConfig> dmzrevamp$parseMobEffects(JsonElement arrayElement, JsonElement singleElement) {
        JsonElement element = arrayElement != null && !arrayElement.isJsonNull() ? arrayElement : singleElement;
        if (element == null || element.isJsonNull()) {
            return List.of();
        }
        List<QuestMobEffectConfig> effects = new ArrayList<>();
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement entry : array) {
                dmzrevamp$addEffect(effects, entry);
            }
        } else {
            dmzrevamp$addEffect(effects, element);
        }
        return effects;
    }

    @Unique
    private static void dmzrevamp$addEffect(List<QuestMobEffectConfig> effects, JsonElement element) {
        if (element != null && element.isJsonObject()) {
            QuestMobEffectConfig config = QuestMobEffectConfig.fromJson(element.getAsJsonObject());
            if (config != null) {
                effects.add(config);
            }
        }
    }
}
