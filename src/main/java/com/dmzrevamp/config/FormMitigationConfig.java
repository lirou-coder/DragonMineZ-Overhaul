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

/** Configuration for damage reduction provided by active forms. */
public final class FormMitigationConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("dmzrevamp");
    private static final Path PATH = CONFIG_DIR.resolve("formMitigation.json");
    private static final Path LEGACY_PATH = CONFIG_DIR.resolve("adaptiveDefenseMoreConfigured.json");
    private static Config cached = new Config();

    private FormMitigationConfig() {
    }

    public static synchronized void initialize() {
        reload();
    }

    public static synchronized void reload() {
        try {
            Files.createDirectories(CONFIG_DIR);
            if (!Files.exists(PATH) && Files.exists(LEGACY_PATH)) {
                migrateLegacyFile();
            }
            if (!Files.exists(PATH)) {
                Files.writeString(PATH, GSON.toJson(new Config()));
            }
            JsonObject json = JsonParser.parseString(Files.readString(PATH)).getAsJsonObject();
            Config loaded = GSON.fromJson(json, Config.class);
            cached = (loaded == null ? new Config() : loaded).sanitize();
            Files.writeString(PATH, GSON.toJson(cached));
        } catch (Exception exception) {
            LOGGER.warn("Could not load formMitigation.json: {}", exception.getMessage());
            cached = new Config();
        }
    }

    private static void migrateLegacyFile() throws Exception {
        JsonObject old = JsonParser.parseString(Files.readString(LEGACY_PATH)).getAsJsonObject();
        JsonObject form = old.has("formReduction") && old.get("formReduction").isJsonObject()
                ? old.getAsJsonObject("formReduction")
                : old;
        JsonObject replacement = new JsonObject();
        replacement.add("formReduction", form.deepCopy());
        Files.writeString(PATH, GSON.toJson(replacement));
    }

    public static Config get() {
        return cached;
    }

    public static final class Config {
        public FormReduction formReduction = new FormReduction();

        private Config sanitize() {
            if (formReduction == null) formReduction = new FormReduction();
            formReduction.sanitize();
            return this;
        }
    }

    public static final class FormReduction {
        public boolean enabled = true;
        @SerializedName("FormReductionCap")
        public double formReductionCap = 0.7D;
        public double formDivisorMulti = 0.05D;
        public boolean masteryInfluence = true;
        public double zeroMasteryMulti = 0.5D;
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

    private static double clamp(double value, double min, double max, double fallback) {
        return Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : fallback;
    }
}
