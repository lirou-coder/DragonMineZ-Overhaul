package com.dmzrevamp.config;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.GeneralServerConfig;
import com.dragonminez.server.util.GravityLogic;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;

public final class WeightMovementPenaltyConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("dmzrevamp");
    private static final Path CONFIG_PATH = CONFIG_DIR.resolve("weightMovementPenalty.json");
    private static final Path LEGACY_CONFIG_PATH = CONFIG_DIR.resolve("weigthMovementPenalty.json");
    private static final int CURRENT_CONFIG_VERSION = 3;

    private static Config cached = Config.createDefault();

    private WeightMovementPenaltyConfig() {
    }

    public static synchronized void initialize() {
        loadFromDisk();
    }

    public static synchronized void reload() {
        loadFromDisk();
    }

    public static boolean isEnabled() {
        return cached.enabled;
    }

    public static double weightPenaltyOrOriginal(Player player) {
        Double configuredPenalty = configuredWeightPenalty(player);
        return configuredPenalty != null ? configuredPenalty : GravityLogic.getWeightPenaltyFactor(player);
    }

    public static Double configuredWeightPenalty(Player player) {
        if (!cached.enabled || player == null) {
            return null;
        }

        GeneralServerConfig serverConfig = ConfigManager.getServerConfig();
        GeneralServerConfig.GravityConfig gravity = serverConfig == null ? null : serverConfig.getGravity();
        if (gravity == null || !Boolean.TRUE.equals(gravity.getTpEnabled()) || GravityLogic.getTotalWeight(player) <= 0) {
            return 0D;
        }

        int idealWeight = GravityLogic.getIdealWeight(player);
        if (idealWeight <= 0) {
            return 0D;
        }

        double ratio = GravityLogic.getEffectiveWeight(player) / (double) idealWeight;
        double penalty = penaltyForRatio(ratio, gravity, cached);
        double relief = GravityLogic.getGravityRoomReliefFraction(player);
        if (relief > 0D) {
            penalty *= 1D - clamp01(relief);
        }
        return clamp01(penalty);
    }

    public static double flightPenaltyMultiplier() {
        return cached.flightPenaltyMultiplier;
    }

    private static double penaltyForRatio(double ratio, GeneralServerConfig.GravityConfig gravity, Config config) {
        if (!Double.isFinite(ratio) || ratio <= 0D) {
            return 0D;
        }

        double idealLow = positiveOrDefault(gravity.getTpIdealRatioLow(), 0.75D);
        double idealHigh = positiveOrDefault(gravity.getTpIdealRatioHigh(), idealLow);
        double overload = positiveOrDefault(gravity.getTpOverloadRatio(), idealHigh);
        double noPenaltyRatio = Math.max(0D, idealLow * config.noPenaltyIdealWeightMulti);

        // Very light weights stay free, then the equipped weight ratio slides through the configured penalty points.
        if (ratio <= noPenaltyRatio) {
            return 0D;
        }
        if (ratio <= idealLow) {
            return interpolate(config.penaltyMinimum, config.penaltyIdealLow, progress(ratio, noPenaltyRatio, idealLow));
        }
        if (ratio <= idealHigh) {
            return interpolate(config.penaltyIdealLow, config.penaltyIdealHigh, progress(ratio, idealLow, idealHigh));
        }
        if (ratio <= overload) {
            return interpolate(config.penaltyIdealHigh, config.penaltyOverloaded, progress(ratio, idealHigh, overload));
        }
        return config.penaltyOverloaded;
    }

    private static synchronized void loadFromDisk() {
        try {
            if (!Files.exists(CONFIG_PATH) && Files.exists(LEGACY_CONFIG_PATH)) {
                Files.move(LEGACY_CONFIG_PATH, CONFIG_PATH);
            }
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
            LOGGER.warn("Could not load Dragon Mine Z: Overhaul weight movement penalty config: {}", exception.getMessage());
            cached = Config.createDefault();
        }
    }

    private static void writeDefault() {
        try {
            Files.createDirectories(CONFIG_DIR);
            Files.writeString(CONFIG_PATH, GSON.toJson(Config.createDefault()));
        } catch (Exception exception) {
            LOGGER.warn("Could not create Dragon Mine Z: Overhaul weight movement penalty config: {}", exception.getMessage());
        }
    }

    private static double positiveOrDefault(Double value, double fallback) {
        if (value == null || !Double.isFinite(value) || value <= 0D) {
            return Math.max(0.000001D, fallback);
        }
        return value;
    }

    private static double progress(double value, double start, double end) {
        double range = end - start;
        if (range <= 0D) {
            return 1D;
        }
        return clamp01((value - start) / range);
    }

    private static double interpolate(double start, double end, double progress) {
        return start + ((end - start) * clamp01(progress));
    }

    private static double clamp01(double value) {
        if (!Double.isFinite(value)) {
            return 0D;
        }
        return Math.max(0D, Math.min(0.99D, value));
    }

    public static final class Config {
        public int configVersion = CURRENT_CONFIG_VERSION;
        public boolean enabled = true;
        public double noPenaltyIdealWeightMulti = 0.01D;
        public double penaltyMinimum = 0.005D;
        public double penaltyIdealLow = 0.3D;
        public double penaltyIdealHigh = 0.5D;
        public double penaltyOverloaded = 0.9D;
        public double flightPenaltyMultiplier = 0.5D;

        private static Config createDefault() {
            return new Config();
        }

        private Config sanitized() {
            configVersion = CURRENT_CONFIG_VERSION;
            noPenaltyIdealWeightMulti = sanitizeNonNegative(noPenaltyIdealWeightMulti, 0.01D);
            penaltyMinimum = sanitizePenalty(penaltyMinimum, 0.005D);
            penaltyIdealLow = sanitizePenalty(penaltyIdealLow, 0.3D);
            penaltyIdealHigh = sanitizePenalty(penaltyIdealHigh, 0.5D);
            penaltyOverloaded = sanitizePenalty(penaltyOverloaded, 0.9D);
            flightPenaltyMultiplier = sanitizeNonNegative(flightPenaltyMultiplier, 0.5D);
            return this;
        }

        private static double sanitizeNonNegative(double value, double fallback) {
            if (!Double.isFinite(value) || value < 0D) {
                return fallback;
            }
            return value;
        }

        private static double sanitizePenalty(double value, double fallback) {
            if (!Double.isFinite(value) || value < 0D) {
                return fallback;
            }
            return Math.min(0.99D, value);
        }
    }
}
