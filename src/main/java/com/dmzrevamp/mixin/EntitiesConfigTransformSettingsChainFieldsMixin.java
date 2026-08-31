package com.dmzrevamp.mixin;

import com.dragonminez.common.config.EntitiesConfig;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = EntitiesConfig.TransformSettings.class, remap = false)
public abstract class EntitiesConfigTransformSettingsChainFieldsMixin {

    @Unique @SerializedName(value="Transform2Health", alternate={"transform2Health"}) private Double dmzrevamp$t2Health;
    @Unique @SerializedName(value="Transform2HealthMulti", alternate={"Transform2HealthMultiplier","transform2HealthMulti"}) private Double dmzrevamp$t2HealthMulti;
    @Unique @SerializedName(value="Transform2MeleeDamage", alternate={"transform2MeleeDamage"}) private Double dmzrevamp$t2Melee;
    @Unique @SerializedName(value="Transform2MeleeDamageMulti", alternate={"Transform2MeleeDamageMultiplier","Transform2MeleeMulti","transform2MeleeDamageMulti"}) private Double dmzrevamp$t2MeleeMulti;
    @Unique @SerializedName(value="Transform2KiDamage", alternate={"transform2KiDamage"}) private Double dmzrevamp$t2Ki;
    @Unique @SerializedName(value="Transform2KiDamageMulti", alternate={"Transform2KiDamageMultiplier","Transform2KiMulti","transform2KiDamageMulti"}) private Double dmzrevamp$t2KiMulti;
    @Unique @SerializedName(value="Transform2Armor", alternate={"transform2Armor"}) private Double dmzrevamp$t2Armor;
    @Unique @SerializedName(value="Transform2ArmorMulti", alternate={"Transform2ArmorMultiplier","transform2ArmorMulti"}) private Double dmzrevamp$t2ArmorMulti;
    @Unique @SerializedName(value="Transform2ArmorToughness", alternate={"transform2ArmorToughness"}) private Double dmzrevamp$t2Toughness;
    @Unique @SerializedName(value="Transform2ArmorToughnessMulti", alternate={"Transform2ArmorToughnessMultiplier","transform2ArmorToughnessMulti"}) private Double dmzrevamp$t2ToughnessMulti;
    @Unique @SerializedName(value="Transform2Protection", alternate={"transform2Protection"}) private Double dmzrevamp$t2Protection;
    @Unique @SerializedName(value="Transform2ProtectionMulti", alternate={"Transform2ProtectionMultiplier","transform2ProtectionMulti"}) private Double dmzrevamp$t2ProtectionMulti;
    @Unique @SerializedName(value="Transform2MovementSpeed", alternate={"transform2MovementSpeed"}) private Double dmzrevamp$t2Speed;
    @Unique @SerializedName(value="Transform2MovementSpeedMulti", alternate={"Transform2MovementSpeedMultiplier","transform2MovementSpeedMulti"}) private Double dmzrevamp$t2SpeedMulti;
    @Unique @SerializedName(value="Transform2TriggerPercent", alternate={"Transform2TriggerHealthPercent","transform2TriggerPercent"}) private Double dmzrevamp$t2Trigger;
    @Unique @SerializedName(value="Transform2MobEffects", alternate={"Transform2MobEffect","transform2MobEffects","transform2MobEffect"}) private JsonElement dmzrevamp$t2Effects;
    @Unique @SerializedName(value="Transform3Health", alternate={"transform3Health"}) private Double dmzrevamp$t3Health;
    @Unique @SerializedName(value="Transform3HealthMulti", alternate={"Transform3HealthMultiplier","transform3HealthMulti"}) private Double dmzrevamp$t3HealthMulti;
    @Unique @SerializedName(value="Transform3MeleeDamage", alternate={"transform3MeleeDamage"}) private Double dmzrevamp$t3Melee;
    @Unique @SerializedName(value="Transform3MeleeDamageMulti", alternate={"Transform3MeleeDamageMultiplier","Transform3MeleeMulti","transform3MeleeDamageMulti"}) private Double dmzrevamp$t3MeleeMulti;
    @Unique @SerializedName(value="Transform3KiDamage", alternate={"transform3KiDamage"}) private Double dmzrevamp$t3Ki;
    @Unique @SerializedName(value="Transform3KiDamageMulti", alternate={"Transform3KiDamageMultiplier","Transform3KiMulti","transform3KiDamageMulti"}) private Double dmzrevamp$t3KiMulti;
    @Unique @SerializedName(value="Transform3Armor", alternate={"transform3Armor"}) private Double dmzrevamp$t3Armor;
    @Unique @SerializedName(value="Transform3ArmorMulti", alternate={"Transform3ArmorMultiplier","transform3ArmorMulti"}) private Double dmzrevamp$t3ArmorMulti;
    @Unique @SerializedName(value="Transform3ArmorToughness", alternate={"transform3ArmorToughness"}) private Double dmzrevamp$t3Toughness;
    @Unique @SerializedName(value="Transform3ArmorToughnessMulti", alternate={"Transform3ArmorToughnessMultiplier","transform3ArmorToughnessMulti"}) private Double dmzrevamp$t3ToughnessMulti;
    @Unique @SerializedName(value="Transform3Protection", alternate={"transform3Protection"}) private Double dmzrevamp$t3Protection;
    @Unique @SerializedName(value="Transform3ProtectionMulti", alternate={"Transform3ProtectionMultiplier","transform3ProtectionMulti"}) private Double dmzrevamp$t3ProtectionMulti;
    @Unique @SerializedName(value="Transform3MovementSpeed", alternate={"transform3MovementSpeed"}) private Double dmzrevamp$t3Speed;
    @Unique @SerializedName(value="Transform3MovementSpeedMulti", alternate={"Transform3MovementSpeedMultiplier","transform3MovementSpeedMulti"}) private Double dmzrevamp$t3SpeedMulti;
    @Unique @SerializedName(value="Transform3TriggerPercent", alternate={"Transform3TriggerHealthPercent","transform3TriggerPercent"}) private Double dmzrevamp$t3Trigger;
    @Unique @SerializedName(value="Transform3MobEffects", alternate={"Transform3MobEffect","transform3MobEffects","transform3MobEffect"}) private JsonElement dmzrevamp$t3Effects;
}

