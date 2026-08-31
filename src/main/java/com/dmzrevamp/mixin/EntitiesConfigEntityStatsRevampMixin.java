package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.entities.RevampEntityStatsData;
import com.dmzrevamp.revamp.quest.QuestMobEffectConfig;
import com.dragonminez.common.config.EntitiesConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = EntitiesConfig.EntityStats.class, remap = false)
public abstract class EntitiesConfigEntityStatsRevampMixin implements RevampEntityStatsData {
    @Unique
    @SerializedName(value = "Armor", alternate = {"armor"})
    private Double dmzrevamp$armor;
    @Unique
    @SerializedName(value = "ArmorToughness", alternate = {"armorToughness"})
    private Double dmzrevamp$armorToughness;
    @Unique
    @SerializedName(value = "Protection", alternate = {"protection"})
    private Double dmzrevamp$protection;
    @Unique
    @SerializedName(value = "movementSpeed", alternate = {"MovementSpeed"})
    private Double dmzrevamp$movementSpeed;
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
    private List<QuestMobEffectConfig> dmzrevamp$mobEffects = List.of();
    @Unique
    private List<QuestMobEffectConfig> dmzrevamp$transformMobEffects = List.of();
    @Unique
    @SerializedName(value = "mobEffects", alternate = {"MobEffects"})
    private JsonElement dmzrevamp$mobEffectsJson;
    @Unique
    @SerializedName(value = "mobEffect", alternate = {"MobEffect"})
    private JsonElement dmzrevamp$mobEffectJson;
    @Unique
    @SerializedName(value = "TransformMobEffects", alternate = {"transformMobEffects"})
    private JsonElement dmzrevamp$transformMobEffectsJson;
    @Unique
    @SerializedName(value = "TransformMobEffect", alternate = {"transformMobEffect"})
    private JsonElement dmzrevamp$transformMobEffectJson;

    @Override
    public Double dmzrevamp$getArmor() {
        return dmzrevamp$armor;
    }

    @Override
    public void dmzrevamp$setArmor(Double value) {
        dmzrevamp$armor = value;
    }

    @Override
    public Double dmzrevamp$getArmorToughness() {
        return dmzrevamp$armorToughness;
    }

    @Override
    public void dmzrevamp$setArmorToughness(Double value) {
        dmzrevamp$armorToughness = value;
    }

    @Override
    public Double dmzrevamp$getProtection() {
        return dmzrevamp$protection;
    }

    @Override
    public void dmzrevamp$setProtection(Double value) {
        dmzrevamp$protection = value;
    }

    @Override
    public Double dmzrevamp$getMovementSpeed() {
        return dmzrevamp$movementSpeed;
    }

    @Override
    public void dmzrevamp$setMovementSpeed(Double value) {
        dmzrevamp$movementSpeed = value;
    }

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
    public List<QuestMobEffectConfig> dmzrevamp$getMobEffects() {
        List<QuestMobEffectConfig> saved = dmzrevamp$mobEffects == null ? List.of() : dmzrevamp$mobEffects;
        return saved.isEmpty() ? dmzrevamp$parseMobEffects(dmzrevamp$mobEffectsJson, dmzrevamp$mobEffectJson) : saved;
    }

    @Override
    public void dmzrevamp$setMobEffects(List<QuestMobEffectConfig> effects) {
        dmzrevamp$mobEffects = effects == null ? List.of() : List.copyOf(effects);
        dmzrevamp$mobEffectsJson = null;
        dmzrevamp$mobEffectJson = null;
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
