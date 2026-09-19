package com.dmzrevamp.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ExtraDifficultiesConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FMLPaths.CONFIGDIR.get().resolve("dmzrevamp").resolve("extraDifficulties.json");
    private static Config cached = new Config();

    private ExtraDifficultiesConfig() {}

    public static Config get() { return cached; }
    public static void initialize() { reload(); }

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
            LOGGER.warn("Could not load extraDifficulties.json: {}", exception.getMessage());
            cached = new Config();
        }
    }

    public static Values values(String difficultyName) {
        return "YOU_MUST_DIE".equals(difficultyName) ? get().youMustDie : get().nightmare;
    }

    public static final class Config {
        public Values nightmare = new Values(4D, 2.5D, 1.5D, 1.5D);
        public Values youMustDie = new Values(10D, 6.5D, 2.5D, 2.5D);

        private Config sanitize() {
            nightmare = Values.sanitize(nightmare, new Values(4D, 2.5D, 1.5D, 1.5D));
            youMustDie = Values.sanitize(youMustDie, new Values(10D, 6.5D, 2.5D, 2.5D));
            return this;
        }
    }

    public static final class Values {
        public double hpMultiplier;
        public double damageMultiplier;
        public double tpMultiplier;
        public double questRewardMultiplier;

        public Values() {}
        private Values(double hp, double damage, double tp, double reward) {
            hpMultiplier = hp; damageMultiplier = damage; tpMultiplier = tp; questRewardMultiplier = reward;
        }

        private static Values sanitize(Values value, Values fallback) {
            if (value == null) return fallback;
            value.hpMultiplier = positive(value.hpMultiplier, fallback.hpMultiplier);
            value.damageMultiplier = positive(value.damageMultiplier, fallback.damageMultiplier);
            value.tpMultiplier = positive(value.tpMultiplier, fallback.tpMultiplier);
            value.questRewardMultiplier = positive(value.questRewardMultiplier, fallback.questRewardMultiplier);
            return value;
        }

        private static double positive(double value, double fallback) {
            return Double.isFinite(value) && value >= 0D ? value : fallback;
        }
    }
}
