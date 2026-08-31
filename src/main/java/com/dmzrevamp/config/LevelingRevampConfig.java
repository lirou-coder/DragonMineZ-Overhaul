package com.dmzrevamp.config;

import com.dmzrevamp.compat.DmzPrestigeCompat;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;

public final class LevelingRevampConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FMLPaths.CONFIGDIR.get().resolve("dmzrevamp").resolve("LevelingRevamp.json");
    private static Config cached = new Config();

    private LevelingRevampConfig() {
    }

    public static synchronized void initialize() {
        reload();
    }

    public static synchronized void reload() {
        loadFromDisk();
        DmzPrestigeCompat.notifyOnlinePlayersIfDisabled();
    }

    public static Config get() {
        return cached;
    }

    public static boolean levelsEnabled() {
        return get().levelsAndAttributes.enabled;
    }

    public static boolean prestigeEnabled() {
        Config config = get();
        return !DmzPrestigeCompat.isPresent()
                && config.levelsAndAttributes.enabled
                && config.Prestige.enabled;
    }

    private static synchronized void loadFromDisk() {
        try {
            if (!Files.exists(PATH)) {
                Files.createDirectories(PATH.getParent());
                Files.writeString(PATH, GSON.toJson(new Config()));
            }
            Config loaded = GSON.fromJson(Files.readString(PATH), Config.class);
            cached = (loaded == null ? new Config() : loaded).sanitize();
            DmzPrestigeCompat.forceDisabled(cached);
            Files.writeString(PATH, GSON.toJson(cached));
        } catch (Exception exception) {
            LOGGER.warn("Could not load LevelingRevamp.json: {}", exception.getMessage());
            cached = new Config();
            DmzPrestigeCompat.forceDisabled(cached);
        }
    }

    public static final class Config {
        public LevelsAndAttributes levelsAndAttributes = new LevelsAndAttributes();
        public Prestige Prestige = new Prestige();

        private Config sanitize() {
            if (levelsAndAttributes == null) levelsAndAttributes = new LevelsAndAttributes();
            if (Prestige == null) Prestige = new Prestige();
            levelsAndAttributes.sanitize();
            Prestige.sanitize(levelsAndAttributes.maxLevel);
            return this;
        }
    }

    public static final class LevelsAndAttributes {
        public boolean enabled = true;
        public int maxLevel = 500_000;
        public int maxAttribute = -1;
        public int levelUpPerPoints = 6;
        public boolean customHexStatCurve = true;

        private void sanitize() {
            maxLevel = Math.max(1, maxLevel);
            levelUpPerPoints = Math.max(1, levelUpPerPoints);
            if (maxAttribute != -1) {
                maxAttribute = Math.max(1, maxAttribute);
                if (maxAttribute < maxLevel) {
                    maxAttribute = maxLevel > Integer.MAX_VALUE - 10
                            ? Integer.MAX_VALUE
                            : maxLevel + 10;
                }
            }
        }
    }

    public static final class Prestige {
        public boolean enabled = true;
        public int initialLevelCap = 50_000;
        public int maxPrestigeCount = 10;
        public boolean prestigeHexStatChange = true;
        public boolean usePrestigeMaxAttribute = true;
        public double scaleBonusPerPrestige = 0.5D;
        public double TPBonusPerPrestige = 1.0D;
        public double masteryBonusPerPrestige = 1.0D;
        public double storyRewardBonusPerPrestige = 0.5D;
        public double storyDifficultyIncreasePerPrestige = 0.75D;
        public boolean keepSkillsOnPrestige = false;
        public boolean keepFormsOnPrestige = false;
        public boolean bonusesLostOnPrestige = true;
        public boolean statRevertToInitialOnPrestige = true;
        public double statPercentageLossOnPrestige = 0.95D;
        public boolean resetQuestsOnPrestige = true;

        private void sanitize(int maxLevel) {
            initialLevelCap = Math.max(1, Math.min(maxLevel, initialLevelCap));
            maxPrestigeCount = Math.max(1, maxPrestigeCount);
            scaleBonusPerPrestige = nonNegativeFinite(scaleBonusPerPrestige, 0.1D);
            TPBonusPerPrestige = nonNegativeFinite(TPBonusPerPrestige, 0.1D);
            masteryBonusPerPrestige = nonNegativeFinite(masteryBonusPerPrestige, 0.1D);
            storyRewardBonusPerPrestige = nonNegativeFinite(storyRewardBonusPerPrestige, 0.1D);
            storyDifficultyIncreasePerPrestige = nonNegativeFinite(storyDifficultyIncreasePerPrestige, 0.1D);
            statPercentageLossOnPrestige = finiteRange(statPercentageLossOnPrestige, 0.95D, 0D, 1D);
        }
    }

    private static double nonNegativeFinite(double value, double fallback) {
        return Double.isFinite(value) ? Math.max(0D, value) : fallback;
    }

    private static double finiteRange(double value, double fallback, double minimum, double maximum) {
        return Double.isFinite(value) ? Math.max(minimum, Math.min(maximum, value)) : fallback;
    }
}
