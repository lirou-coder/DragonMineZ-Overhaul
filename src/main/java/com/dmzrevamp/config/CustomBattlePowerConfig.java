package com.dmzrevamp.config;

import com.dmzrevamp.compat.DmzSparkingCompat;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class CustomBattlePowerConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("dmzrevamp");
    private static final Path CONFIG_PATH = CONFIG_DIR.resolve("custom_BP.json");
    private static final Path LEGACY_CONFIG_PATH = CONFIG_DIR.resolve("custom BP.json");
    private static final double DEFAULT_REFERENCE_MULTIPLIER = 1200D;
    private static final double DEFAULT_TOTAL_STATS_DIVISOR = DmzSparkingCompat.isLoadedEarly() ? 100D : 500D;
    private static final double DEFAULT_EXPONENT = 1.2D;
    private static final int CURRENT_CONFIG_VERSION = 3;

    private static Config cached = Config.createDefault();

    private CustomBattlePowerConfig() {
    }

    public static Config get() {
        return cached;
    }

    public static synchronized void initialize() {
        loadFromDisk();
    }

    public static synchronized void reload() {
        loadFromDisk();
    }

    private static synchronized void loadFromDisk() {
        try {
            migrateLegacyPath();
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
            LOGGER.warn("Could not load Dragon Mine Z: Overhaul custom BP config: {}", exception.getMessage());
            cached = Config.createDefault();
        }
    }

    private static void writeDefault() {
        try {
            Files.createDirectories(CONFIG_DIR);
            Files.writeString(CONFIG_PATH, GSON.toJson(Config.createDefault()));
        } catch (Exception exception) {
            LOGGER.warn("Could not create Dragon Mine Z: Overhaul custom BP config: {}", exception.getMessage());
        }
    }

    private static void migrateLegacyPath() {
        try {
            if (Files.exists(CONFIG_PATH) || !Files.exists(LEGACY_CONFIG_PATH)) {
                return;
            }
            Files.createDirectories(CONFIG_DIR);
            Files.move(LEGACY_CONFIG_PATH, CONFIG_PATH);
        } catch (Exception exception) {
            LOGGER.warn("Could not migrate Dragon Mine Z: Overhaul custom BP config to custom_BP.json: {}", exception.getMessage());
        }
    }

    public static final class Config {
        public int configVersion = CURRENT_CONFIG_VERSION;
        public double referenceMultiplier = DEFAULT_REFERENCE_MULTIPLIER;
        public double totalStatsDivisor = DEFAULT_TOTAL_STATS_DIVISOR;
        public double exponent = DEFAULT_EXPONENT;
        public Map<String, StatRule> playerStats = defaultPlayerStats();
        public Map<String, StatRule> mobStats = defaultMobStats();

        private static Config createDefault() {
            return new Config();
        }

        private Config sanitized() {
            int loadedVersion = configVersion;
            boolean legacyConfig = loadedVersion < CURRENT_CONFIG_VERSION;
            configVersion = CURRENT_CONFIG_VERSION;
            if (!Double.isFinite(referenceMultiplier) || referenceMultiplier <= 0D) {
                referenceMultiplier = DEFAULT_REFERENCE_MULTIPLIER;
            }
            if (!Double.isFinite(totalStatsDivisor) || totalStatsDivisor <= 0D) {
                totalStatsDivisor = DEFAULT_TOTAL_STATS_DIVISOR;
            }
            if (!Double.isFinite(exponent) || exponent <= 0D) {
                exponent = DEFAULT_EXPONENT;
            }
            playerStats = sanitizeRules(playerStats, defaultPlayerStats(), false);
            mobStats = sanitizeRules(mobStats, defaultMobStats(), legacyConfig);
            if (loadedVersion < 3) {
                StatRule resistance = mobStats.get("resistance");
                if (resistance != null && closeTo(resistance.weight, 4D)) {
                    resistance.weight = 20D;
                }
            }
            return this;
        }
    }

    public static final class StatRule {
        public boolean enabled;
        public double weight;

        public StatRule() {
        }

        private StatRule(boolean enabled, double weight) {
            this.enabled = enabled;
            this.weight = weight;
        }
    }

    public static double weightedValue(Map<String, StatRule> rules, String key, double value) {
        if (rules == null || !Double.isFinite(value)) {
            return 0D;
        }
        StatRule rule = rules.get(key);
        if (rule == null || !rule.enabled || !Double.isFinite(rule.weight)) {
            return 0D;
        }
        return Math.max(0D, value) * rule.weight;
    }

    private static Map<String, StatRule> defaultPlayerStats() {
        Map<String, StatRule> rules = new LinkedHashMap<>();
        rules.put("meleeDamage", enabled(1D));
        rules.put("strikeDamage", enabled(1D));
        rules.put("maxStamina", new StatRule(!DmzSparkingCompat.isLoadedEarly(), 0.5D));
        rules.put("defense", enabled(1D));
        rules.put("maxHealth", enabled(0.5D));
        rules.put("kiDamage", enabled(1D));
        rules.put("maxKi", enabled(0.5D));
        return rules;
    }

    private static Map<String, StatRule> defaultMobStats() {
        Map<String, StatRule> rules = new LinkedHashMap<>();
        rules.put("maxHealth", enabled(0.5D));
        rules.put("attackDamage", enabled(1D));
        rules.put("armor", enabled(4D));
        rules.put("armorToughness", enabled(4D));
        rules.put("protection", enabled(4D));
        rules.put("resistance", enabled(20D));
        rules.put("movementOrFlyingSpeed", enabled(15D));
        rules.put("kiDamage", enabled(1D));
        rules.put("arrowDamage", enabled(0.5D));
        rules.put("autoLevelingProjectileDamage", enabled(0.5D));
        rules.put("autoLevelingExplosionDamage", enabled(1D));
        rules.put("ironsSpellPower", enabled(5D));
        return rules;
    }

    private static Map<String, StatRule> sanitizeRules(Map<String, StatRule> rules, Map<String, StatRule> defaults, boolean upgradeLegacyMobDefaults) {
        Map<String, StatRule> sanitized = copyRules(defaults);
        if (rules != null) {
            migrateAliases(rules);
        }
        if (rules == null) {
            return sanitized;
        }
        for (Map.Entry<String, StatRule> entry : rules.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null) {
                continue;
            }
            StatRule rule = entry.getValue();
            double weight = Double.isFinite(rule.weight) ? rule.weight : 0D;
            if (upgradeLegacyMobDefaults && isLegacyMobWeight(entry.getKey(), weight)) {
                weight = defaults.get(entry.getKey()).weight;
            }
            sanitized.put(entry.getKey(), new StatRule(rule.enabled, weight));
        }
        return sanitized;
    }

    private static void migrateAliases(Map<String, StatRule> rules) {
        if (rules.containsKey("kiBlastDamage") && !rules.containsKey("kiDamage")) {
            rules.put("kiDamage", rules.get("kiBlastDamage"));
        }
    }

    private static boolean isLegacyMobWeight(String key, double weight) {
        return weight <= 1D && (
                "armor".equals(key)
                        || "armorToughness".equals(key)
                        || "protection".equals(key)
                        || "resistance".equals(key)
        );
    }

    private static Map<String, StatRule> copyRules(Map<String, StatRule> source) {
        Map<String, StatRule> copy = new LinkedHashMap<>();
        for (Map.Entry<String, StatRule> entry : source.entrySet()) {
            StatRule rule = entry.getValue();
            copy.put(entry.getKey(), new StatRule(rule.enabled, rule.weight));
        }
        return copy;
    }

    private static boolean closeTo(double value, double expected) {
        return Math.abs(value - expected) < 1.0E-9D;
    }

    private static StatRule enabled(double weight) {
        return new StatRule(true, weight);
    }
}
