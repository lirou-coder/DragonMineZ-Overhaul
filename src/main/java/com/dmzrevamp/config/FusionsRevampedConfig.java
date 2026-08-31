package com.dmzrevamp.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class FusionsRevampedConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("dmzrevamp");
    private static final Path CONFIG_PATH = CONFIG_DIR.resolve("fusionsRevamped.json");
    private static final int CURRENT_CONFIG_VERSION = 1;
    private static final String[] DEFAULT_FUSION_BOOSTS = {"STR", "SKP", "PWR", "DEF", "STM", "VIT", "ENE"};

    private static Config cached = Config.createDefault();

    private FusionsRevampedConfig() {
    }

    public static synchronized void initialize() {
        loadFromDisk();
    }

    public static synchronized void reload() {
        loadFromDisk();
    }

    public static Config get() {
        return cached;
    }

    public static boolean isRevampedEnabled() {
        return get().fusionRevamped.enabled;
    }

    public static boolean canUseDifferentRaceMetamoru() {
        return get().allowDifferentRaceMetamoruFusion;
    }

    public static boolean shouldBypassAndroidMetamoruCheck(boolean bothPlayersAreAndroids) {
        Config config = get();
        return config.allowAndroidFusion || (bothPlayersAreAndroids && config.fusionBetweenAndroids);
    }

    private static synchronized void loadFromDisk() {
        try {
            if (!Files.exists(CONFIG_PATH)) {
                writeDefault();
            }
            String rawConfig = Files.readString(CONFIG_PATH);
            Config loaded = GSON.fromJson(rawConfig, Config.class);
            cached = loaded == null ? Config.createDefault() : loaded.sanitized();
            String normalizedConfig = GSON.toJson(cached);
            if (!normalizedConfig.equals(rawConfig)) {
                Files.writeString(CONFIG_PATH, normalizedConfig);
            }
        } catch (Exception exception) {
            LOGGER.warn("Could not load Dragon Mine Z: Overhaul fusions config: {}", exception.getMessage());
            cached = Config.createDefault();
        }
    }

    private static void writeDefault() {
        try {
            Files.createDirectories(CONFIG_DIR);
            Files.writeString(CONFIG_PATH, GSON.toJson(Config.createDefault()));
        } catch (Exception exception) {
            LOGGER.warn("Could not create Dragon Mine Z: Overhaul fusions config: {}", exception.getMessage());
        }
    }

    public static final class Config {
        public int configVersion = CURRENT_CONFIG_VERSION;
        public boolean allowDifferentRaceMetamoruFusion = true;
        public FusionRevamped fusionRevamped = new FusionRevamped();
        public boolean allowAndroidFusion = false;
        public boolean fusionBetweenAndroids = true;

        private static Config createDefault() {
            return new Config();
        }

        private Config sanitized() {
            configVersion = CURRENT_CONFIG_VERSION;
            if (fusionRevamped == null) {
                fusionRevamped = new FusionRevamped();
            }
            fusionRevamped = fusionRevamped.sanitized();
            return this;
        }
    }

    public static final class FusionRevamped {
        public boolean enabled = true;
        public double metamoruMinBonus = 0.5D;
        public double metamoruMaxBonus = 1.0D;
        public double potaraMinBonus = 0.75D;
        public double potaraMaxBonus = 1.25D;
        public String[] fusionBoosts = DEFAULT_FUSION_BOOSTS.clone();
        public boolean scaleAddition = true;
        public boolean transformationsLowerFusedTimer = true;
        public double fusionTransformationDecreaseMultiplier = 1.0D;

        private FusionRevamped sanitized() {
            metamoruMinBonus = sanitizeBonus(metamoruMinBonus, 0.5D);
            metamoruMaxBonus = sanitizeBonus(metamoruMaxBonus, 1.0D);
            potaraMinBonus = sanitizeBonus(potaraMinBonus, 0.75D);
            potaraMaxBonus = sanitizeBonus(potaraMaxBonus, 1.25D);
            fusionTransformationDecreaseMultiplier = sanitizeBonus(fusionTransformationDecreaseMultiplier, 1.0D);
            fusionBoosts = sanitizeBoosts(fusionBoosts);
            return this;
        }

        public boolean boosts(String stat) {
            String normalized = normalizeStat(stat);
            for (String boosted : fusionBoosts) {
                if (normalizeStat(boosted).equals(normalized)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static String normalizeStat(String stat) {
        String normalized = stat == null ? "" : stat.trim().toUpperCase(Locale.ROOT);
        return "SPD".equals(normalized) ? "SKP" : normalized;
    }

    private static String[] sanitizeBoosts(String[] boosts) {
        if (boosts == null || boosts.length == 0) {
            return DEFAULT_FUSION_BOOSTS.clone();
        }
        Set<String> sanitized = new LinkedHashSet<>();
        for (String boost : boosts) {
            String normalized = normalizeStat(boost);
            if (isSupportedStat(normalized)) {
                sanitized.add(normalized);
            }
        }
        return sanitized.isEmpty() ? DEFAULT_FUSION_BOOSTS.clone() : sanitized.toArray(String[]::new);
    }

    private static boolean isSupportedStat(String stat) {
        return switch (stat) {
            case "STR", "SKP", "PWR", "DEF", "STM", "VIT", "ENE" -> true;
            default -> false;
        };
    }

    private static double sanitizeBonus(double value, double fallback) {
        if (!Double.isFinite(value) || value < 0D) {
            return fallback;
        }
        return value;
    }
}
