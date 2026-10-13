package com.dmzrevamp.mixin;

import com.dragonminez.common.diagnostics.JsonLoadReport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Set;

@Mixin(targets = "com.dragonminez.common.diagnostics.JsonSchema", remap = false)
public abstract class JsonSchemaRevampRaidTournamentMixin {
    private static final Set<String> BASE_FIELDS = Set.of(
            "Defense", "defense", "Armor", "armor", "ArmorToughness", "armorToughness", "Protection", "protection",
            "movementSpeed", "MovementSpeed", "canTransform", "TransformEntity", "TransformOverride", "TransformOveride",
            "TransformHealth", "TransformHealthMulti", "TransformHealthMultiplier",
            "TransformMeleeDamage", "TransformMeleeDamageMulti", "TransformMeleeMulti", "TransformMeleeDamageMultiplier",
            "TransformKiDamage", "TransformKiDamageMulti", "TransformKiMulti", "TransformKiDamageMultiplier",
            "TransformTriggerPercent", "TransformTriggerHealthPercent", "TransformDefense", "transformDefense",
            "TransformDefenseMulti", "TransformDefenseMultiplier", "TransformArmor", "TransformArmorMulti", "TransformArmorMultiplier",
            "TransformArmorToughness", "TransformArmorToughnessMulti", "TransformArmorToughnessMultiplier",
            "TransformProtection", "TransformProtectionMulti", "TransformProtectionMultiplier",
            "TransformMovementSpeed", "TransformMovementSpeedMulti", "TransformMovementSpeedMultiplier",
            "mobEffects", "mobEffect", "TransformMobEffects", "TransformMobEffect"
    );

    @Redirect(method = "check",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/diagnostics/JsonLoadReport;error(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V"),
            require = 0)
    private static void dmzrevamp$ignoreKnownOverhaulFields(String source, String file, String message) {
        String key = unknownKey(message);
        if (key != null && isOverhaulField(key)) return;
        JsonLoadReport.error(source, file, message);
    }

    private static String unknownKey(String message) {
        if (message == null || !message.startsWith("unknown field '") ) return null;
        int end = message.indexOf('\'', 15);
        return end > 15 ? message.substring(15, end) : null;
    }

    private static boolean isOverhaulField(String key) {
        if (BASE_FIELDS.contains(key) || key.matches("canTransform[234]")) return true;
        for (int stage = 2; stage <= 4; stage++) {
            String prefix = "Transform" + stage;
            if (key.equals(prefix + "Entity") || key.equals(prefix + "Override") || key.equals(prefix + "Overide")
                    || key.equals(prefix + "MobEffects") || key.equals(prefix + "MobEffect")) return true;
            for (String suffix : new String[]{
                    "Health", "HealthMulti", "HealthMultiplier", "MeleeDamage", "MeleeDamageMulti", "MeleeDamageMultiplier", "MeleeMulti",
                    "KiDamage", "KiDamageMulti", "KiDamageMultiplier", "KiMulti", "Defense", "DefenseMulti", "DefenseMultiplier",
                    "Armor", "ArmorMulti", "ArmorMultiplier", "ArmorToughness", "ArmorToughnessMulti", "ArmorToughnessMultiplier",
                    "Protection", "ProtectionMulti", "ProtectionMultiplier", "MovementSpeed", "movementSpeed", "MovementSpeedMulti",
                    "MovementSpeedMultiplier", "TriggerPercent", "TriggerHealthPercent"}) {
                if (key.equals(prefix + suffix)) return true;
            }
        }
        return false;
    }
}
