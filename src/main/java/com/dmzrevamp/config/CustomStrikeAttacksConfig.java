package com.dmzrevamp.config;

import com.dmzrevamp.revamp.strike.CustomStrikeType;
import com.dmzrevamp.revamp.strike.RevampStrikeAttackData;
import com.dmzrevamp.revamp.strike.StrikeAttackTemplates;
import com.dragonminez.common.stats.techniques.StrikeAttackData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** DMZ-techniques-style settings for Overhaul-created Strike Attacks. */
public final class CustomStrikeAttacksConfig {
    private static final String CONFIG_VERSION = "1.2.0";
    private static final int LEGACY_RACIAL_COOLDOWN_TICKS = 80;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FMLPaths.CONFIGDIR.get().resolve("dmzrevamp").resolve("customStrikeAttacks.json");
    private static Config cached = defaults();

    private CustomStrikeAttacksConfig() {
    }

    public static synchronized void initialize() {
        reload();
    }

    public static synchronized void reload() {
        try {
            Config defaults = defaults();
            if (!Files.exists(PATH)) {
                Files.createDirectories(PATH.getParent());
                Files.writeString(PATH, GSON.toJson(defaults));
            }
            Config loaded = GSON.fromJson(Files.readString(PATH), Config.class);
            cached = sanitizeAndMerge(loaded, defaults);
            Files.writeString(PATH, GSON.toJson(cached));
        } catch (Exception exception) {
            LOGGER.warn("Could not load customStrikeAttacks.json: {}", exception.getMessage());
            cached = defaults();
        }
    }

    public static StrikeSettings resolve(StrikeAttackData strike) {
        Config config = cached;
        String id = normalize(strike.getId());
        StrikeSettings exact = config.strikeAttacks.get(id);
        if (exact != null) {
            return exact;
        }
        if (strike instanceof RevampStrikeAttackData revamp) {
            String type = normalize(revamp.dmzrevamp$getStrikeType().translationSuffix());
            StrikeSettings byType = config.strikeAttacks.get(type);
            if (byType != null) {
                return byType;
            }
        }
        return StrikeSettings.defaults(240);
    }

    private static Config defaults() {
        Config config = new Config();
        for (CustomStrikeType type : CustomStrikeType.values()) {
            config.strikeAttacks.put(normalize(type.translationSuffix()),
                    StrikeSettings.defaults(type.isEvasive() ? 400 : 240));
        }
        config.strikeAttacks.put(StrikeAttackTemplates.ANDROID_ABSORPTION, StrikeSettings.defaults(1200));
        config.strikeAttacks.put(StrikeAttackTemplates.SLEEP_RECOVERY, StrikeSettings.defaults(1800));
        config.strikeAttacks.put(StrikeAttackTemplates.NAMEKIAN_REGENERATION, StrikeSettings.defaults(1200));
        return config;
    }

    private static Config sanitizeAndMerge(Config loaded, Config defaults) {
        if (loaded == null) {
            return defaults;
        }
        boolean migrateLegacyRacialCooldowns = !CONFIG_VERSION.equals(loaded.configVersion);
        loaded.configVersion = CONFIG_VERSION;
        Map<String, StrikeSettings> merged = new LinkedHashMap<>();
        if (loaded.strikeAttacks != null) {
            loaded.strikeAttacks.forEach((key, value) -> {
                if (key != null && !key.isBlank()) {
                    merged.put(normalize(key), (value == null ? StrikeSettings.defaults(240) : value).sanitize());
                }
            });
        }
        defaults.strikeAttacks.forEach(merged::putIfAbsent);
        if (migrateLegacyRacialCooldowns) {
            // DMZ's normal Strike defaults use a 5x XP gain multiplier. Older
            // custom configs were accidentally generated with 1x.
            merged.values().forEach(settings -> {
                if (settings.xpGainMultiplier == 1.0D) settings.xpGainMultiplier = 5.0D;
            });
        }
        if (migrateLegacyRacialCooldowns) {
            migrateLegacyCooldown(merged, StrikeAttackTemplates.ANDROID_ABSORPTION, 1200);
            migrateLegacyCooldown(merged, StrikeAttackTemplates.SLEEP_RECOVERY, 1800);
        }
        loaded.strikeAttacks = merged;
        return loaded;
    }

    private static void migrateLegacyCooldown(Map<String, StrikeSettings> settings, String id, int newDefault) {
        StrikeSettings strike = settings.get(id);
        if (strike != null && strike.cooldownTicks == LEGACY_RACIAL_COOLDOWN_TICKS) {
            strike.cooldownTicks = newDefault;
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    public static final class Config {
        public String configVersion = CONFIG_VERSION;
        @SerializedName("StrikeAttacks")
        public Map<String, StrikeSettings> strikeAttacks = new LinkedHashMap<>();
    }

    public static final class StrikeSettings {
        public int minXPCost = 100;
        public int maxXPCost = -1;
        public double xpCostMultiplier = 1.0D;
        public double xpGainMultiplier = 5.0D;
        public int xpGainPerHit = 1;
        public int xpGainPerKill = 3;
        public double kiCostMultiplier = 1.0D;
        public double damageMultiplier = 1.0D;
        public int castTimeTicks = 0;
        public int cooldownTicks = 240;

        private static StrikeSettings defaults(int cooldownTicks) {
            StrikeSettings settings = new StrikeSettings();
            settings.cooldownTicks = cooldownTicks;
            return settings;
        }

        private StrikeSettings sanitize() {
            minXPCost = Math.max(0, minXPCost);
            maxXPCost = maxXPCost < -1 ? -1 : maxXPCost;
            xpCostMultiplier = nonNegative(xpCostMultiplier, 1.0D);
            xpGainMultiplier = nonNegative(xpGainMultiplier, 1.0D);
            xpGainPerHit = Math.max(0, xpGainPerHit);
            xpGainPerKill = Math.max(0, xpGainPerKill);
            kiCostMultiplier = nonNegative(kiCostMultiplier, 1.0D);
            damageMultiplier = nonNegative(damageMultiplier, 1.0D);
            castTimeTicks = Math.max(0, castTimeTicks);
            cooldownTicks = Math.max(0, cooldownTicks);
            return this;
        }

        private static double nonNegative(double value, double fallback) {
            return Double.isFinite(value) ? Math.max(0.0D, value) : fallback;
        }
    }
}
