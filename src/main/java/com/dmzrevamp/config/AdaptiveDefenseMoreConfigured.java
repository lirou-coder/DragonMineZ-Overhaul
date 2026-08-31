package com.dmzrevamp.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
            if (!Files.exists(PATH)) {
                Files.createDirectories(PATH.getParent());
                Files.writeString(PATH, GSON.toJson(new Config()));
            }
            Config loaded = GSON.fromJson(Files.readString(PATH), Config.class);
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

    public static final class Config {
        public boolean enable = true;
        public double adaptativeMitigationParityRatio = 1.0D;
        public double adaptativeMitigationParityValue = 0.6D;
        public double adaptativeMitigationZeroRatio = 6.0D;
        public double adaptativeDefenseMitigationCap = 0.8D;
        public double cancelDamageMitigationThreshold = 20.0D;
        public double adaptiveDefenseCapRatio = 4.0D;
        public double adaptiveDefenseKiAttackEfficiency = 1.0D;
        public double adaptiveDefenseStrikeAttackEfficiency = 1.0D;

        private Config sanitize() {
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
            return this;
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
