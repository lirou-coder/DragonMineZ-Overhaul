package com.dmzrevamp.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;

public final class AdaptiveDefenseMoreConfigured {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FMLPaths.CONFIGDIR.get().resolve("dmzrevamp")
            .resolve("adaptiveDefenseMoreConfigured.json");
    private static Config cached = new Config();

    private AdaptiveDefenseMoreConfigured() {
    }

    public static synchronized void initialize() {
        reload();
    }

    public static synchronized void reload() {
        try {
            Files.createDirectories(PATH.getParent());
            if (!Files.exists(PATH)) {
                Files.writeString(PATH, GSON.toJson(new Config()));
            }
            JsonObject json = JsonParser.parseString(Files.readString(PATH)).getAsJsonObject();
            if (!json.has("adaptiveDefense") && !json.has("formReduction")) {
                json = migrateLegacyConfig(json);
            }
            Config loaded = GSON.fromJson(json, Config.class);
            cached = (loaded == null ? new Config() : loaded).sanitize();
            Files.writeString(PATH, GSON.toJson(cached));
        } catch (Exception exception) {
            LOGGER.warn("Could not load adaptiveDefenseMoreConfigured.json: {}", exception.getMessage());
            cached = new Config();
        }
    }

    public static Config get() {
        return cached;
    }

    /** Converts the previous flat file while keeping every explicitly configured value. */
    private static JsonObject migrateLegacyConfig(JsonObject legacy) {
        JsonObject result = new JsonObject();
        JsonObject adaptive = new JsonObject();
        JsonObject form = new JsonObject();

        copy(legacy, adaptive, "enable", "enabled");
        copy(legacy, adaptive, "adaptativeMitigationParityRatio", "adaptativeMitigationParityRatio");
        copy(legacy, adaptive, "adaptativeMitigationParityValue", "adaptativeMitigationParityValue");
        copy(legacy, adaptive, "adaptativeMitigationZeroRatio", "adaptativeMitigationZeroRatio");
        copy(legacy, adaptive, "adaptativeDefenseMitigationCap", "adaptativeDefenseMitigationCap");
        copy(legacy, adaptive, "cancelDamageMitigationThreshold", "cancelDamageMitigationThreshold");
        copy(legacy, adaptive, "adaptiveDefenseCapRatio", "adaptiveDefenseCapRatio");
        copy(legacy, adaptive, "adaptiveDefenseKiAttackEfficiency", "adaptiveDefenseKiAttackEfficiency");
        copy(legacy, adaptive, "adaptiveDefenseStrikeAttackEfficiency", "adaptiveDefenseStrikeAttackEfficiency");

        copy(legacy, form, "damageDivisorEnabled", "enabled");
        copy(legacy, form, "formDivisorMulti", "formDivisorMulti");
        copy(legacy, form, "FormReductionCap", "FormReductionCap");
        copy(legacy, form, "masteryInfluence", "masteryInfluence");
        copy(legacy, form, "zeroMasteryMulti", "zeroMasteryMulti");
        copy(legacy, form, "currentHealthInfluence", "currentHealthInfluence");
        copy(legacy, form, "zeroHealthMulti", "zeroHealthMulti");
        copy(legacy, form, "currentKiInfluence", "currentKiInfluence");
        copy(legacy, form, "zeroKiMulti", "zeroKiMulti");
        copy(legacy, form, "currentStaminaInfluence", "currentStaminaInfluence");
        copy(legacy, form, "zeroStaminaInfluence", "zeroStaminaInfluence");

        result.add("adaptiveDefense", adaptive);
        result.add("formReduction", form);
        return result;
    }

    private static void copy(JsonObject source, JsonObject target, String sourceKey, String targetKey) {
        if (source.has(sourceKey)) {
            target.add(targetKey, source.get(sourceKey));
        }
    }

    public static final class Config {
        public AdaptiveDefense adaptiveDefense = new AdaptiveDefense();
        public FormReduction formReduction = new FormReduction();

        private Config sanitize() {
            if (adaptiveDefense == null) adaptiveDefense = new AdaptiveDefense();
            if (formReduction == null) formReduction = new FormReduction();
            adaptiveDefense.sanitize();
            formReduction.sanitize();
            return this;
        }
    }

    public static final class AdaptiveDefense {
        /** Enables the configured adaptive defense curve and full-negation behavior. */
        public boolean enabled = true;
        public double adaptativeMitigationParityRatio = 1.0D;
        public double adaptativeMitigationParityValue = 0.6D;
        public double adaptativeMitigationZeroRatio = 20.0D;
        public double adaptativeDefenseMitigationCap = 0.8D;
        public double cancelDamageMitigationThreshold = 20.0D;
        public double adaptiveDefenseCapRatio = 15.0D;
        public double adaptiveDefenseKiAttackEfficiency = 1.0D;
        public double adaptiveDefenseStrikeAttackEfficiency = 1.0D;

        private void sanitize() {
            adaptativeMitigationParityRatio = positive(adaptativeMitigationParityRatio, 1.0D);
            adaptativeMitigationParityValue = clamp(adaptativeMitigationParityValue, 0D, 1D, 0.35D);
            adaptativeMitigationZeroRatio = Math.max(
                    adaptativeMitigationParityRatio + 0.0001D,
                    positive(adaptativeMitigationZeroRatio, 5.0D)
            );
            adaptativeDefenseMitigationCap = clamp(adaptativeDefenseMitigationCap, 0D, 1D, 0.60D);
            cancelDamageMitigationThreshold = positive(cancelDamageMitigationThreshold, 20.0D);
            adaptiveDefenseCapRatio = positive(adaptiveDefenseCapRatio, 10.0D);
            adaptiveDefenseKiAttackEfficiency = nonNegative(adaptiveDefenseKiAttackEfficiency, 1.0D);
            adaptiveDefenseStrikeAttackEfficiency = nonNegative(adaptiveDefenseStrikeAttackEfficiency, 1.0D);
        }
    }

    public static final class FormReduction {
        /** Enables the post-mitigation damage reduction provided by active forms. */
        public boolean enabled = true;
        @SerializedName("FormReductionCap")
        public double formReductionCap = 0.7D;
        public double formDivisorMulti = 0.05D;
        public boolean masteryInfluence = true;
        public double zeroMasteryMulti = 0.25D;
        public boolean currentHealthInfluence = false;
        public double zeroHealthMulti = 0.5D;
        public boolean currentKiInfluence = true;
        public double zeroKiMulti = 0.0D;
        public boolean currentStaminaInfluence = false;
        public double zeroStaminaInfluence = 0.5D;

        private void sanitize() {
            formReductionCap = clamp(formReductionCap, 0D, 1D, 0.7D);
            formDivisorMulti = clamp(formDivisorMulti, 0D, 1D, 0.05D);
            zeroMasteryMulti = clamp(zeroMasteryMulti, 0D, 1D, 0.5D);
            zeroHealthMulti = clamp(zeroHealthMulti, 0D, 1D, 0.5D);
            zeroKiMulti = clamp(zeroKiMulti, 0D, 1D, 0.5D);
            zeroStaminaInfluence = clamp(zeroStaminaInfluence, 0D, 1D, 0.5D);
        }
    }

    private static double positive(double value, double fallback) {
        return Double.isFinite(value) && value > 0D ? value : fallback;
    }

    private static double nonNegative(double value, double fallback) {
        return Double.isFinite(value) && value >= 0D ? value : fallback;
    }

    private static double clamp(double value, double min, double max, double fallback) {
        return Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : fallback;
    }
}
