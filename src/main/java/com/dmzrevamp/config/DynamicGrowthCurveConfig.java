package com.dmzrevamp.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;

public final class DynamicGrowthCurveConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("dmzrevamp");
    private static final Path CONFIG_PATH = CONFIG_DIR.resolve("DynamicGrowthCurve.json");
    private static final int CURRENT_CONFIG_VERSION = 2;
    private static final double DEFAULT_BASE_XP = 70D;
    private static final double DEFAULT_CURRENT_STAT_MULTIPLIER = 2D;
    private static final double DEFAULT_STAT_EXPONENT = 1.35D;
    private static final double DEFAULT_XP_PERCENTAGE_PER_GRAVITY_PER_SECOND = 0.005D;

    private static Config cached = Config.createDefault();

    private DynamicGrowthCurveConfig() {
    }

    public static synchronized void initialize() {
        loadFromDisk();
    }

    public static synchronized void reload() {
        loadFromDisk();
    }

    public static int requiredXpOrOriginal(int currentStat, int originalValue) {
        if (!cached.enabled) {
            return originalValue;
        }

        double safeStat = Math.max(0D, currentStat);
        double value = cached.baseXp
                + (safeStat * cached.currentStatMultiplier)
                + Math.floor(Math.pow(safeStat, cached.statExponent));
        if (!Double.isFinite(value)) {
            return originalValue;
        }
        return clampToInt(Math.max(1D, Math.floor(value)));
    }

    public static double xpPercentagePerGravityPerSecond() {
        return cached.xpPercentagePerGravityPerSecond;
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
            LOGGER.warn("Could not load Dragon Mine Z: Overhaul Dynamic Growth curve config: {}", exception.getMessage());
            cached = Config.createDefault();
        }
    }

    private static void writeDefault() {
        try {
            Files.createDirectories(CONFIG_DIR);
            Files.writeString(CONFIG_PATH, GSON.toJson(Config.createDefault()));
        } catch (Exception exception) {
            LOGGER.warn("Could not create Dragon Mine Z: Overhaul Dynamic Growth curve config: {}", exception.getMessage());
        }
    }

    private static int clampToInt(double value) {
        if (value >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (value <= Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) value;
    }

    public static final class Config {
        public int configVersion = CURRENT_CONFIG_VERSION;
        public boolean enabled = true;
        public double baseXp = DEFAULT_BASE_XP;
        public double currentStatMultiplier = DEFAULT_CURRENT_STAT_MULTIPLIER;
        public double statExponent = DEFAULT_STAT_EXPONENT;
        public double xpPercentagePerGravityPerSecond = DEFAULT_XP_PERCENTAGE_PER_GRAVITY_PER_SECOND;

        private static Config createDefault() {
            return new Config();
        }

        private Config sanitized() {
            configVersion = CURRENT_CONFIG_VERSION;
            if (!Double.isFinite(baseXp) || baseXp < 0D) {
                baseXp = DEFAULT_BASE_XP;
            }
            if (!Double.isFinite(currentStatMultiplier) || currentStatMultiplier < 0D) {
                currentStatMultiplier = DEFAULT_CURRENT_STAT_MULTIPLIER;
            }
            if (!Double.isFinite(statExponent) || statExponent <= 0D) {
                statExponent = DEFAULT_STAT_EXPONENT;
            }
            if (!Double.isFinite(xpPercentagePerGravityPerSecond) || xpPercentagePerGravityPerSecond < 0D) {
                xpPercentagePerGravityPerSecond = DEFAULT_XP_PERCENTAGE_PER_GRAVITY_PER_SECOND;
            }
            return this;
        }
    }
}
