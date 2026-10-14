package com.dmzrevamp.mixin;

import com.dragonminez.common.diagnostics.JsonLoadReport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Locale;
import java.util.Set;

@Mixin(targets = "com.dragonminez.common.diagnostics.JsonSchema", remap = false)
public abstract class JsonSchemaRevampRaidTournamentMixin {
    private static final Set<String> BASE_FIELDS = Set.of(
            "defense", "armor", "armortoughness", "protection", "movementspeed", "cantransform",
            "transformentity", "transformoverride", "transformoveride",
            "mobeffects", "mobeffect", "transformmobeffects", "transformmobeffect"
    );

    private static final Set<String> TRANSFORM_SUFFIXES = Set.of(
            "health", "healthmulti", "healthmultiplier",
            "meleedamage", "meleedamagemulti", "meleedamagemultiplier", "meleemulti",
            "kidamage", "kidamagemulti", "kidamagemultiplier", "kimulti",
            "defense", "defensemulti", "defensemultiplier",
            "armor", "armormulti", "armormultiplier",
            "armortoughness", "armortoughnessmulti", "armortoughnessmultiplier",
            "protection", "protectionmulti", "protectionmultiplier",
            "movementspeed", "movementspeedmulti", "movementspeedmultiplier",
            "triggerpercent", "triggerhealthpercent", "mobeffects", "mobeffect"
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
        if (key == null) return false;
        String normalized = key.toLowerCase(Locale.ROOT);
        if (BASE_FIELDS.contains(normalized) || normalized.matches("cantransform[234]")) return true;
        if (isTransformEntityField(normalized, "transform")) return true;
        for (int stage = 2; stage <= 4; stage++) {
            String prefix = "transform" + stage;
            if (isTransformEntityField(normalized, prefix)) return true;
            for (String suffix : TRANSFORM_SUFFIXES) if (normalized.equals(prefix + suffix)) return true;
        }
        for (String suffix : TRANSFORM_SUFFIXES) if (normalized.equals("transform" + suffix)) return true;
        return false;
    }

    private static boolean isTransformEntityField(String key, String prefix) {
        return key.equals(prefix + "entity") || key.equals(prefix + "override") || key.equals(prefix + "overide")
                || key.equals(prefix + "mobeffects") || key.equals(prefix + "mobeffect");
    }
}
