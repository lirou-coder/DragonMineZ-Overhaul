package com.dmzrevamp.revamp.classes;

import com.dmzrevamp.config.racial.DmzRevampRacialConfigs;
import com.dmzrevamp.compat.SduCompat;
import com.dmzrevamp.revamp.classes.skills.CustomClassPassives;
import com.dragonminez.common.config.RaceStatsConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class DmzClassConfigManager {
    public static final String RACE_DEFAULT_CLASS = "race";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CLASSES_DIR = FMLPaths.CONFIGDIR.get().resolve("dragonminez").resolve("classes");
    private static final String DISPLAY_NAME_KEY = "displayName";
    private static final String DISPLAY_COLOR_KEY = "displayColor";
    private static final String EXCLUSIVE_RACES_KEY = "exclusiveRaces";
    private static final String CONFIG_VERSION_KEY = "configVersion";
    private static final String SAIRENS_DMZ_WORLD_MODID = "sairens_dmz_world";
    private static final String CUSTOM_PASSIVE_TUTORIAL = "customRacialTutorial.txt";
    private static final String[] DEFAULT_CLASS_IDS = {
            "warrior",
            "berserker",
            "spiritualist",
            "martialartist",
            "cleric",
            "paladin",
            "tank",
            "speedster",
            "duelist",
            "kiassassin",
            "potentialist"
    };
    private static final Set<String> SAIRENS_CLASS_IDS = Set.of(
            "celestialfist", "supremelightning", "unbreakablefortress", "ghoststep",
            "spiritualcontrol", "dragonsfury", "stardestroyer", "limitbreaker",
            "unbeatablebeast", "nodojo"
    );
    private static final Set<RaceStatsConfig> PREPARED_CONFIGS = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Map<String, RaceStatsConfig.ClassStats> ADDON_CLASS_DEFAULTS = new LinkedHashMap<>();
    private static final Map<String, ClassMetadata> ADDON_CLASS_METADATA = new LinkedHashMap<>();
    private static final Map<String, RaceStatsConfig.ClassStats> ADDON_RACE_DEFAULTS = new LinkedHashMap<>();
    private static final Map<String, RaceStatsConfig.ClassStats> CLASS_STATS = new LinkedHashMap<>();
    private static final Map<String, ClassMetadata> CLASS_METADATA = new LinkedHashMap<>();

    private static boolean loaded;

    // Creates a new DmzClassConfigManager instance.
    private DmzClassConfigManager() {
    }

    // Returns a snapshot of the loaded class stat configs.
    public static synchronized Map<String, RaceStatsConfig.ClassStats> getClassStatsSnapshot() {
        ensureLoaded();
        return Collections.unmodifiableMap(new LinkedHashMap<>(CLASS_STATS));
    }

    /** Returns a complete JSON copy suitable for SDU's global class editor. */
    public static synchronized JsonObject getEditableClassJson(String classId) {
        ensureLoaded();
        String normalized = normalize(classId);
        RaceStatsConfig.ClassStats stats = CLASS_STATS.get(normalized);
        if (stats == null) {
            stats = createZeroClassStats();
        }

        JsonObject json = GSON.toJsonTree(copyStats(stats)).getAsJsonObject();
        ClassMetadata metadata = CLASS_METADATA.getOrDefault(normalized, defaultMetadata(normalized));
        json.addProperty(CONFIG_VERSION_KEY, RaceStatsConfig.CURRENT_VERSION);
        json.addProperty(DISPLAY_NAME_KEY, metadata.displayName());
        json.addProperty(DISPLAY_COLOR_KEY, displayColorAsHex(metadata.displayColor()));
        json.add(EXCLUSIVE_RACES_KEY, toJsonArray(metadata.exclusiveRaces()));
        return json;
    }

    public static synchronized boolean hasConfiguredClass(String classId) {
        ensureLoaded();
        return CLASS_STATS.containsKey(normalize(classId));
    }

    /** Custom passives must not replace built-in Overhaul or Sairens class passives. */
    public static boolean canEditCustomPassive(String classId) {
        String normalized = normalize(classId);
        return !normalized.isEmpty() && !isBuiltInClassId(normalized) && !SAIRENS_CLASS_IDS.contains(normalized);
    }

    /** Persists an SDU-edited global class and refreshes all merged race/class data. */
    public static synchronized boolean saveEditableClassJson(String classId, JsonObject json) {
        String normalized = normalize(classId);
        if (normalized.isEmpty() || RACE_DEFAULT_CLASS.equals(normalized) || json == null) {
            return false;
        }

        try {
            normalizeCustomPassiveBoolean(json);
            RaceStatsConfig.ClassStats stats = sanitize(GSON.fromJson(json, RaceStatsConfig.ClassStats.class));
            ClassMetadata metadata = readMetadata(normalized, json, defaultMetadata(normalized));
            saveClassConfig(CLASSES_DIR.resolve(normalized + ".json"), stats, metadata);
            reload();
            return true;
        } catch (RuntimeException | IOException ignored) {
            return false;
        }
    }

    public static synchronized RaceStatsConfig.ClassStats getConfiguredClassStats(String classId) {
        ensureLoaded();
        return CLASS_STATS.get(normalize(classId));
    }

    public static synchronized void reload() {
        loaded = false;
        PREPARED_CONFIGS.clear();
        CLASS_STATS.clear();
        CLASS_METADATA.clear();
        ensureLoaded();
    }

    public static synchronized void registerClassDefault(String classId, RaceStatsConfig.ClassStats defaultStats, ClassMetadata defaultMetadata) {
        String normalized = normalize(classId);
        if (normalized.isEmpty() || RACE_DEFAULT_CLASS.equals(normalized) || defaultStats == null) {
            return;
        }

        ClassMetadata metadata = defaultMetadata != null ? defaultMetadata : defaultMetadata(normalized);
        ADDON_CLASS_DEFAULTS.put(normalized, copyStats(sanitize(defaultStats)));
        ADDON_CLASS_METADATA.put(normalized, metadata);
        if (loaded) {
            loadRegisteredClassDefault(normalized, defaultStats, metadata);
            PREPARED_CONFIGS.clear();
        }
    }

    public static synchronized void registerRaceDefaultStats(String raceId, RaceStatsConfig.ClassStats defaultStats) {
        String normalized = normalize(raceId);
        if (normalized.isEmpty() || defaultStats == null) {
            return;
        }
        ADDON_RACE_DEFAULTS.put(normalized, copyStats(sanitize(defaultStats)));
        PREPARED_CONFIGS.clear();
    }

    // Returns the configured metadata for a class id.
    public static synchronized ClassMetadata getClassMetadata(String classId) {
        ensureLoaded();
        return CLASS_METADATA.getOrDefault(normalize(classId), defaultMetadata(classId));
    }

    // Returns the configured display name for a class id.
    public static synchronized String getDisplayName(String classId) {
        return getClassMetadata(classId).displayName();
    }

    // Returns the configured display color for a class id.
    public static synchronized int getDisplayColor(String classId) {
        return getClassMetadata(classId).colorValue();
    }

    // Applies the separated class configs to every loaded race stats config.
    public static synchronized void prepareAllRaceStats(Map<String, RaceStatsConfig> raceStatsById) {
        if (raceStatsById == null) {
            return;
        }

        raceStatsById.forEach(DmzClassConfigManager::prepareRaceStats);
    }

    // Replaces a race's class map with the race default merged with each allowed class config.
    public static synchronized void prepareRaceStats(String raceId, RaceStatsConfig raceStats) {
        if (raceStats == null || PREPARED_CONFIGS.contains(raceStats)) {
            return;
        }

        ensureLoaded();

        Map<String, RaceStatsConfig.ClassStats> raceClasses = raceStats.getClasses();
        RaceStatsConfig.ClassStats raceDefault = findRaceDefaultStats(raceClasses);
        if (raceDefault == null) {
            raceDefault = createRaceDefaultStats(raceId);
        }
        if (raceDefault == null) {
            return;
        }

        String normalizedRace = normalize(raceId);
        applyRaceRegenOverrides(normalizedRace, raceDefault);
        RaceStatsConfig.ClassStats finalRaceDefault = raceDefault;
        raceClasses.clear();
        CLASS_STATS.forEach((classId, classStats) -> {
            if (!SduCompat.isClassSuppressed(classId) && isClassAllowedForRace(classId, normalizedRace)) {
                raceClasses.put(classId, merge(finalRaceDefault, classStats));
            }
        });
        PREPARED_CONFIGS.add(raceStats);
    }

    public static synchronized RaceStatsConfig createSeparatedRaceStats(String raceId, RaceStatsConfig sourceStats) {
        RaceStatsConfig separatedStats = new RaceStatsConfig();
        String configVersion = sourceStats != null && sourceStats.getConfigVersion() != null && !sourceStats.getConfigVersion().isBlank() ? sourceStats.getConfigVersion() : RaceStatsConfig.CURRENT_VERSION;
        separatedStats.setConfigVersion(configVersion);
        separatedStats.getClasses().clear();

        RaceStatsConfig.ClassStats raceDefault = sourceStats != null ? findRaceDefaultStats(sourceStats.getClasses()) : null;
        if (raceDefault == null) {
            raceDefault = createRaceDefaultStats(raceId);
        }
        if (raceDefault != null) {
            separatedStats.getClasses().put(RACE_DEFAULT_CLASS, copyStats(sanitize(raceDefault)));
        }
        return separatedStats;
    }

    public static synchronized boolean isVanillaClassStatsConfig(RaceStatsConfig sourceStats) {
        if (sourceStats == null || sourceStats.getClasses() == null || sourceStats.getClasses().isEmpty()) {
            return false;
        }
        if (sourceStats.getClasses().size() == 1 && findRaceDefaultStats(sourceStats.getClasses()) != null) {
            return false;
        }

        for (String classId : sourceStats.getClasses().keySet()) {
            if (isBuiltInClassId(classId)) {
                return true;
            }
        }
        return sourceStats.getClasses().size() > 1 && findRaceDefaultStats(sourceStats.getClasses()) == null;
    }

    // Finds the race-only base stats entry in a race stats config.
    private static RaceStatsConfig.ClassStats findRaceDefaultStats(Map<String, RaceStatsConfig.ClassStats> raceClasses) {
        if (raceClasses == null || raceClasses.isEmpty()) {
            return null;
        }

        for (Map.Entry<String, RaceStatsConfig.ClassStats> entry : raceClasses.entrySet()) {
            String key = normalize(entry.getKey());
            if (RACE_DEFAULT_CLASS.equals(key) || "default".equals(key) || "racedefault".equals(key) || "race_default".equals(key) || "base".equals(key)) {
                return entry.getValue();
            }
        }

        return null;
    }

    // Creates the built-in race-only default stats for the separated race config.
    public static RaceStatsConfig.ClassStats createRaceDefaultStats(String raceId) {
        String normalized = normalize(raceId);
        RaceStatsConfig.ClassStats addonDefault = ADDON_RACE_DEFAULTS.get(normalized);
        if (addonDefault != null) {
            return copyStats(addonDefault);
        }

        return switch (normalized) {
            case "human" -> createRaceStats(2, 3, 1, 0, 2, 5, 0.5D, 0.5D, 0.4D, 0.6D, 0.6D, 0.7D, 2.0D);
            case "android" -> createRaceStats(4, 3, 2, 2, 2, 0, 0.7D, 0.7D, 0.7D, 0.7D, 0.7D, 0.7D, 1.4D);
            case "saiyan" -> createRaceStats(5, 3, 2, 1, 1, 1, 0.7D, 1.0D, 0.6D, 0.8D, 0.4D, 0.5D, 0.8D);
            case "frostdemon" -> createRaceStats(0, 3, 1, 0, 5, 4, 0.1D, 0.7D, 0.5D, 0.3D, 0.3D, 1.0D, 2.0D);
            case "namekian" -> createRaceStats(2, 2, 3, 2, 2, 2, 0.8D, 0.3D, 1.0D, 0.9D, 1.0D, 0.4D, 1.4D);
            case "majin" -> createRaceStats(3, 2, 2, 1, 2, 3, 0.7D, 0.6D, 1.0D, 0.6D, 0.8D, 0.5D, 1.6D);
            case "bioandroid" -> createRaceStats(2, 3, 1, 1, 2, 3, 0.5D, 0.6D, 0.6D, 0.6D, 0.6D, 0.4D, 1.6D);
            default -> createGenericRaceDefaultStats();
        };
    }

    private static RaceStatsConfig.ClassStats createGenericRaceDefaultStats() {
        return createRaceStats(5, 5, 5, 5, 5, 5, 0.6D, 0.6D, 0.6D, 0.6D, 0.6D, 0.6D, 0.6D);
    }

    private static void applyRaceRegenOverrides(String normalizedRace, RaceStatsConfig.ClassStats raceDefault) {
        double multiplier = switch (normalizedRace) {
            case "human", "saiyan", "frostdemon", "namekian", "majin" -> 0.5D;
            case "bioandroid" -> DmzRevampRacialConfigs.bioAndroid().healthRegenMultiplier;
            default -> 1.0D;
        };
        if (multiplier == 1.0D || raceDefault == null) {
            return;
        }
        if (raceDefault.getBaseHp5() != null) {
            raceDefault.setBaseHp5(raceDefault.getBaseHp5() * multiplier);
        }
        if (raceDefault.getHp5VitScaling() != null) {
            raceDefault.setHp5VitScaling(raceDefault.getHp5VitScaling() * multiplier);
        }
    }

    // Loads class configs. Existing config files are not migrated in place.
    private static void ensureLoaded() {
        if (loaded) {
            return;
        }

        try {
            Files.createDirectories(CLASSES_DIR);
            writeCustomPassiveTutorial();
            CLASS_STATS.clear();
            CLASS_METADATA.clear();

            if (shouldLoadBuiltInClassDefaults()) {
                for (String classId : DEFAULT_CLASS_IDS) {
                    Path path = CLASSES_DIR.resolve(classId + ".json");
                    ClassMetadata defaultMetadata = defaultMetadata(classId);
                    RaceStatsConfig.ClassStats defaultStats = createKnownClassDefault(classId);
                    ClassConfigEntry entry = loadOrCreateClassConfig(path, classId, defaultStats, defaultMetadata);
                    CLASS_STATS.put(classId, entry.stats());
                    CLASS_METADATA.put(classId, entry.metadata());
                    registerCustomPassive(classId, entry);
                }
            }
            ADDON_CLASS_DEFAULTS.forEach((classId, defaultStats) -> loadRegisteredClassDefault(classId, defaultStats, ADDON_CLASS_METADATA.get(classId)));

            try (var files = Files.list(CLASSES_DIR)) {
                files.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".json"))
                        .sorted()
                        .forEach(DmzClassConfigManager::loadExtraClassConfig);
            }
        } catch (IOException ignored) {
        }

        loaded = true;
    }

    private static void loadRegisteredClassDefault(String classId, RaceStatsConfig.ClassStats defaultStats, ClassMetadata defaultMetadata) {
        ClassMetadata metadata = defaultMetadata != null ? defaultMetadata : defaultMetadata(classId);
        Path path = CLASSES_DIR.resolve(classId + ".json");
        ClassConfigEntry entry = loadOrCreateClassConfig(path, classId, defaultStats, metadata);
        CLASS_STATS.put(classId, entry.stats());
        CLASS_METADATA.put(classId, entry.metadata());
        registerCustomPassive(classId, entry);
    }

    // Loads an extra custom class config from the classes folder.
    private static void loadExtraClassConfig(Path path) {
        String fileName = path.getFileName().toString();
        String classId = normalize(fileName.substring(0, fileName.length() - ".json".length()));
        if (classId.isEmpty() || RACE_DEFAULT_CLASS.equals(classId) || CLASS_STATS.containsKey(classId) || "enchanter".equals(classId)) {
            return;
        }
        if (shouldSkipBuiltInClassConfig(classId)) {
            return;
        }

        ClassConfigEntry entry = loadOrCreateClassConfig(path, classId, createZeroClassStats(), defaultMetadata(classId));
        CLASS_STATS.put(classId, entry.stats());
        CLASS_METADATA.put(classId, entry.metadata());
        registerCustomPassive(classId, entry);
    }

    private static boolean shouldLoadBuiltInClassDefaults() {
        return !isSairensDmzWorldLoaded();
    }

    private static boolean shouldSkipBuiltInClassConfig(String classId) {
        return isSairensDmzWorldLoaded() && isBuiltInClassId(classId) && !ADDON_CLASS_DEFAULTS.containsKey(normalize(classId));
    }

    private static boolean isBuiltInClassId(String classId) {
        String normalized = normalize(classId);
        for (String defaultClassId : DEFAULT_CLASS_IDS) {
            if (normalize(defaultClassId).equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSairensDmzWorldLoaded() {
        try {
            ModList modList = ModList.get();
            return modList != null && modList.isLoaded(SAIRENS_DMZ_WORLD_MODID);
        } catch (Exception ignored) {
            return false;
        }
    }

    // Loads a class config file or creates it with defaults if it does not exist.
    private static ClassConfigEntry loadOrCreateClassConfig(Path path, String classId, RaceStatsConfig.ClassStats defaultStats, ClassMetadata defaultMetadata) {
        if (Files.notExists(path)) {
            try {
                saveClassConfig(path, defaultStats, defaultMetadata);
            } catch (IOException ignored) {
            }
            return new ClassConfigEntry(defaultStats, defaultMetadata);
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (!element.isJsonObject()) {
                return new ClassConfigEntry(defaultStats, defaultMetadata);
            }

            JsonObject json = element.getAsJsonObject();
            normalizeCustomPassiveBoolean(json);
            RaceStatsConfig.ClassStats stats = sanitize(GSON.fromJson(json, RaceStatsConfig.ClassStats.class));
            clearMissingClassOnlyMultipliers(json, stats);
            ClassMetadata metadata = readMetadata(classId, json, defaultMetadata);
            boolean legacyPotentialist = false;
            if ("potentialist".equals(normalize(classId))) {
                JsonObject scaling = json.has("statScaling") && json.get("statScaling").isJsonObject()
                        ? json.getAsJsonObject("statScaling") : null;
                if (scaling != null && scaling.has("ENE_scaling")
                        && Math.abs(scaling.get("ENE_scaling").getAsDouble() - 0.6D) < 0.000001D) {
                    stats.getStatScaling().setEnergyScaling(1.2D);
                    legacyPotentialist = true;
                }
                if ("#FFFFFF".equalsIgnoreCase(metadata.displayColor())) {
                    metadata = new ClassMetadata(metadata.classId(), metadata.displayName(), "#FF5555",
                            metadata.exclusiveRaces());
                    legacyPotentialist = true;
                }
            }
            boolean legacyNamedColor = isNamedDisplayColor(readString(json, DISPLAY_COLOR_KEY, ""));
            boolean decimalPassiveEnums = hasDecimalCustomPassiveEnums(json);
            if (!RaceStatsConfig.CURRENT_VERSION.equals(readString(json, CONFIG_VERSION_KEY, ""))
                    || legacyNamedColor || decimalPassiveEnums || legacyPotentialist) {
                try {
                    saveClassConfig(path, stats, metadata);
                } catch (IOException ignored) {
                }
            }
            return new ClassConfigEntry(stats, metadata);
        } catch (Exception ignored) {
            return new ClassConfigEntry(defaultStats, defaultMetadata);
        }
    }

    private static void normalizeCustomPassiveBoolean(JsonObject json) {
        if (json == null || !json.has("passive") || !json.get("passive").isJsonObject()) return;
        JsonObject passive = json.getAsJsonObject("passive");
        if (!passive.has("values") || !passive.get("values").isJsonObject()) return;
        JsonObject values = passive.getAsJsonObject("values");
        String markerKey = null;
        for (String key : values.keySet()) {
            if (key.equalsIgnoreCase("Custom Passive")) {
                markerKey = key;
                break;
            }
        }
        JsonElement marker = markerKey == null ? null : values.get(markerKey);
        if (marker != null && marker.isJsonPrimitive() && marker.getAsJsonPrimitive().isBoolean()) {
            values.remove(markerKey);
            values.addProperty("Custom Passive", marker.getAsBoolean() ? 1D : 0D);
        }
    }

    private static void registerCustomPassive(String classId, ClassConfigEntry entry) {
        if (entry != null && CustomClassPassives.isCustom(entry.stats())) {
            CustomClassPassives.registerClass(classId, entry.metadata().displayName());
        }
    }

    // Clears DMZ constructor defaults for class-only TP multipliers when the config did not explicitly define them.
    private static void clearMissingClassOnlyMultipliers(JsonObject json, RaceStatsConfig.ClassStats stats) {
        if (stats == null) {
            return;
        }
        if (!json.has("tpCostMultiplier")) {
            stats.setTpCostMultiplier(null);
        }
        if (!json.has("tpGainMultiplier")) {
            stats.setTpGainMultiplier(null);
        }
    }

    // Reads class metadata from a JSON object.
    private static ClassMetadata readMetadata(String classId, JsonObject json, ClassMetadata defaults) {
        String displayName = readString(json, DISPLAY_NAME_KEY, defaults.displayName());
        String displayColor = readString(json, DISPLAY_COLOR_KEY, defaults.displayColor());
        List<String> exclusiveRaces = readStringList(json, EXCLUSIVE_RACES_KEY);
        return new ClassMetadata(normalize(classId), displayName, displayColor, exclusiveRaces);
    }

    // Reads a string property with a fallback.
    private static String readString(JsonObject json, String key, String fallback) {
        JsonElement element = json.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : fallback;
    }

    // Reads a list of normalized string values from a JSON array property.
    private static List<String> readStringList(JsonObject json, String key) {
        JsonElement element = json.get(key);
        if (element == null || !element.isJsonArray()) {
            return List.of();
        }

        List<String> values = new ArrayList<>();
        for (JsonElement value : element.getAsJsonArray()) {
            if (value != null && value.isJsonPrimitive()) {
                String normalized = normalize(value.getAsString());
                if (!normalized.isEmpty()) {
                    values.add(normalized);
                }
            }
        }
        return List.copyOf(values);
    }

    // Creates a JSON array from a string list.
    private static JsonArray toJsonArray(List<String> values) {
        JsonArray array = new JsonArray();
        if (values != null) {
            values.forEach(array::add);
        }
        return array;
    }

    // Creates the built-in default stats for a class.
    private static RaceStatsConfig.ClassStats createKnownClassDefault(String classId) {
        String normalizedClassId = normalize(classId);
        RaceStatsConfig.ClassStats stats = switch (normalizedClassId) {
            case "warrior" -> createClassStats(6, 3, 2, 1, 1, 0, 1.0D, 0.7D, 0.4D, 0.6D, 0.6D, 0.3D, 1.0D);
            case "spiritualist" -> createClassStats(0, 2, 1, 0, 6, 3, 0.2D, 0.6D, 0.5D, 0.4D, 0.6D, 1.0D, 2.0D);
            case "berserker" -> createClassStats(7, 3, 2, 2, 0, 0, 1.0D, 0.8D, 0.7D, 0.9D, 0.7D, 0.1D, 0.6D);
            case "martialartist" -> createClassStats(4, 1, 2, 2, 4, 2, 1.0D, 0.8D, 0.7D, 0.7D, 0.2D, 1.0D, 1.2D);
            case "cleric" -> withTp(createClassStats(0, 2, 1, 0, 4, 5, 0.2D, 0.7D, 0.6D, 0.6D, 1.0D, 0.9D, 2.0D), -0.1D, 0.25D);
            case "paladin" -> createClassStats(5, 1, 3, 1, 1, 1, 1.0D, 0.5D, 0.9D, 0.9D, 0.8D, 0.7D, 1.6D);
            case "tank" -> withTp(createClassStats(3, 0, 3, 3, 2, 1, 0.7D, 0.4D, 1.0D, 0.9D, 1.0D, 0.5D, 1.6D), 0.0D, 0.25D);
            case "speedster" -> createClassStats(1, 6, 0, 0, 3, 2, 0.7D, 1.0D, 0.4D, 0.8D, 0.6D, 0.6D, 1.6D);
            case "duelist" -> createClassStats(2, 3, 2, 2, 2, 2, 0.7D, 0.9D, 0.6D, 1.0D, 0.6D, 0.7D, 2.0D);
            case "kiassassin" -> createClassStats(0, 4, 0, 0, 6, 4, 0.0D, 0.8D, 0.3D, 0.5D, 0.5D, 1.0D, 2.0D);
            case "potentialist" -> createClassStats(3, 3, 3, 3, 3, 3, 0.6D, 0.6D, 0.6D, 0.6D, 0.6D, 0.6D, 1.2D);
            default -> createZeroClassStats();
        };
        stats.setPassive(defaultPassive(normalizedClassId));
        return stats;
    }

    // Creates the built-in default metadata for a class.
    private static ClassMetadata defaultMetadata(String classId) {
        return switch (normalize(classId)) {
            case "warrior" -> new ClassMetadata("warrior", "Warrior", "#FF5555", List.of());
            case "berserker" -> new ClassMetadata("berserker", "Berserker", "#AA0000", List.of());
            case "spiritualist" -> new ClassMetadata("spiritualist", "Spiritualist", "#55FFFF", List.of());
            case "martialartist" -> new ClassMetadata("martialartist", "Martial Artist", "#FFFFFF", List.of());
            case "cleric" -> new ClassMetadata("cleric", "Cleric", "#55FF55", List.of());
            case "paladin" -> new ClassMetadata("paladin", "Paladin", "#FFFF55", List.of());
            case "tank" -> new ClassMetadata("tank", "Tank", "#FF55FF", List.of());
            case "speedster" -> new ClassMetadata("speedster", "Speedster", "#00AAAA", List.of());
            case "duelist" -> new ClassMetadata("duelist", "Duelist", "#00AA00", List.of());
            case "kiassassin" -> new ClassMetadata("kiassassin", "Ki Assassin", "#AA00AA", List.of());
            case "potentialist" -> new ClassMetadata("potentialist", "Potentialist", "#FF5555", List.of());
            default -> new ClassMetadata(normalize(classId), titleCase(classId), "#FFFFFF", List.of());
        };
    }

    // Returns true when a class can be selected by a race.
    private static boolean isClassAllowedForRace(String classId, String raceId) {
        ClassMetadata metadata = CLASS_METADATA.get(normalize(classId));
        if (metadata == null || metadata.exclusiveRaces().isEmpty()) {
            return true;
        }
        return metadata.exclusiveRaces().contains(normalize(raceId));
    }

    // Merges race-only stats with class-only stats for selection and gameplay.
    private static RaceStatsConfig.ClassStats merge(RaceStatsConfig.ClassStats raceStats, RaceStatsConfig.ClassStats classStats) {
        RaceStatsConfig.ClassStats merged = new RaceStatsConfig.ClassStats();

        RaceStatsConfig.BaseStats mergedBase = new RaceStatsConfig.BaseStats();
        RaceStatsConfig.BaseStats raceBase = raceStats != null ? raceStats.getBaseStats() : null;
        RaceStatsConfig.BaseStats classBase = classStats != null ? classStats.getBaseStats() : null;
        mergedBase.setStrength(sum(raceBase != null ? raceBase.getStrength() : null, classBase != null ? classBase.getStrength() : null));
        mergedBase.setStrikePower(sum(raceBase != null ? raceBase.getStrikePower() : null, classBase != null ? classBase.getStrikePower() : null));
        mergedBase.setResistance(sum(raceBase != null ? raceBase.getResistance() : null, classBase != null ? classBase.getResistance() : null));
        mergedBase.setVitality(sum(raceBase != null ? raceBase.getVitality() : null, classBase != null ? classBase.getVitality() : null));
        mergedBase.setKiPower(sum(raceBase != null ? raceBase.getKiPower() : null, classBase != null ? classBase.getKiPower() : null));
        mergedBase.setEnergy(sum(raceBase != null ? raceBase.getEnergy() : null, classBase != null ? classBase.getEnergy() : null));
        merged.setBaseStats(mergedBase);

        RaceStatsConfig.StatScaling mergedScaling = new RaceStatsConfig.StatScaling();
        RaceStatsConfig.StatScaling raceScaling = raceStats != null ? raceStats.getStatScaling() : null;
        RaceStatsConfig.StatScaling classScaling = classStats != null ? classStats.getStatScaling() : null;
        mergedScaling.setStrengthScaling(sum(raceScaling != null ? raceScaling.getStrengthScaling() : null, classScaling != null ? classScaling.getStrengthScaling() : null));
        mergedScaling.setStrikePowerScaling(sum(raceScaling != null ? raceScaling.getStrikePowerScaling() : null, classScaling != null ? classScaling.getStrikePowerScaling() : null));
        mergedScaling.setStaminaScaling(sum(raceScaling != null ? raceScaling.getStaminaScaling() : null, classScaling != null ? classScaling.getStaminaScaling() : null));
        mergedScaling.setDefenseScaling(sum(raceScaling != null ? raceScaling.getDefenseScaling() : null, classScaling != null ? classScaling.getDefenseScaling() : null));
        mergedScaling.setVitalityScaling(sum(raceScaling != null ? raceScaling.getVitalityScaling() : null, classScaling != null ? classScaling.getVitalityScaling() : null));
        mergedScaling.setKiPowerScaling(sum(raceScaling != null ? raceScaling.getKiPowerScaling() : null, classScaling != null ? classScaling.getKiPowerScaling() : null));
        mergedScaling.setEnergyScaling(sum(raceScaling != null ? raceScaling.getEnergyScaling() : null, classScaling != null ? classScaling.getEnergyScaling() : null));
        merged.setStatScaling(mergedScaling);

        merged.setBaseHp5(sumNullable(raceStats != null ? raceStats.getBaseHp5() : null, classStats != null ? classStats.getBaseHp5() : null));
        merged.setBaseEp5(sumNullable(raceStats != null ? raceStats.getBaseEp5() : null, classStats != null ? classStats.getBaseEp5() : null));
        merged.setBaseSp5(sumNullable(raceStats != null ? raceStats.getBaseSp5() : null, classStats != null ? classStats.getBaseSp5() : null));
        merged.setHp5VitScaling(sumNullable(raceStats != null ? raceStats.getHp5VitScaling() : null, classStats != null ? classStats.getHp5VitScaling() : null));
        merged.setEp5EneScaling(sumNullable(raceStats != null ? raceStats.getEp5EneScaling() : null, classStats != null ? classStats.getEp5EneScaling() : null));
        merged.setSp5StmScaling(sumNullable(raceStats != null ? raceStats.getSp5StmScaling() : null, classStats != null ? classStats.getSp5StmScaling() : null));
        merged.setTpCostMultiplier(sumNullable(raceStats != null ? raceStats.getTpCostMultiplier() : null, classStats != null ? classStats.getTpCostMultiplier() : null));
        merged.setTpGainMultiplier(sumNullable(raceStats != null ? raceStats.getTpGainMultiplier() : null, classStats != null ? classStats.getTpGainMultiplier() : null));
        if (classStats != null) {
            merged.setPassive(classStats.getPassive());
        }
        return merged;
    }

    // Ensures loaded class stats have non-null nested stat objects.
    private static RaceStatsConfig.ClassStats sanitize(RaceStatsConfig.ClassStats classStats) {
        if (classStats == null) {
            return createZeroClassStats();
        }
        if (classStats.getBaseStats() == null) {
            classStats.setBaseStats(new RaceStatsConfig.BaseStats());
        }
        if (classStats.getStatScaling() == null) {
            classStats.setStatScaling(new RaceStatsConfig.StatScaling());
        }
        return classStats;
    }

    private static RaceStatsConfig.ClassStats copyStats(RaceStatsConfig.ClassStats stats) {
        return sanitize(GSON.fromJson(GSON.toJson(stats), RaceStatsConfig.ClassStats.class));
    }

    // Creates an empty class stats object for custom fallback configs.
    public static RaceStatsConfig.ClassStats createZeroClassStats() {
        return createClassStats(0, 0, 0, 0, 0, 0, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    // Creates the default race stats with current alpha HP, EP, and SP values.
    private static RaceStatsConfig.ClassStats createRaceStats(int strength,
                                                              int strikePower,
                                                              int resistance,
                                                              int vitality,
                                                              int kiPower,
                                                              int energy,
                                                              double strengthScaling,
                                                              double strikePowerScaling,
                                                              double defenseScaling,
                                                              double staminaScaling,
                                                              double vitalityScaling,
                                                              double kiPowerScaling,
                                                              double energyScaling) {
        RaceStatsConfig.ClassStats classStats = createClassStats(strength, strikePower, resistance, vitality, kiPower, energy, strengthScaling, strikePowerScaling, defenseScaling, staminaScaling, vitalityScaling, kiPowerScaling, energyScaling);
        classStats.setBaseHp5(5.0D);
        classStats.setHp5VitScaling(0.06D);
        classStats.setBaseEp5(5.0D);
        classStats.setEp5EneScaling(0.05D);
        classStats.setBaseSp5(14.0D);
        classStats.setSp5StmScaling(0.15D);
        classStats.setTpCostMultiplier(1.0D);
        classStats.setTpGainMultiplier(1.0D);
        return classStats;
    }

    // Creates a class stats object from base attributes and scaling values.
    public static RaceStatsConfig.ClassStats createClassStats(int strength,
                                                              int strikePower,
                                                              int resistance,
                                                              int vitality,
                                                              int kiPower,
                                                              int energy,
                                                              double strengthScaling,
                                                              double strikePowerScaling,
                                                              double defenseScaling,
                                                              double staminaScaling,
                                                              double vitalityScaling,
                                                              double kiPowerScaling,
                                                              double energyScaling) {
        RaceStatsConfig.ClassStats classStats = new RaceStatsConfig.ClassStats();
        RaceStatsConfig.BaseStats baseStats = new RaceStatsConfig.BaseStats();
        baseStats.setStrength(strength);
        baseStats.setStrikePower(strikePower);
        baseStats.setResistance(resistance);
        baseStats.setVitality(vitality);
        baseStats.setKiPower(kiPower);
        baseStats.setEnergy(energy);
        classStats.setBaseStats(baseStats);

        RaceStatsConfig.StatScaling scaling = new RaceStatsConfig.StatScaling();
        scaling.setStrengthScaling(strengthScaling);
        scaling.setStrikePowerScaling(strikePowerScaling);
        scaling.setDefenseScaling(defenseScaling);
        scaling.setStaminaScaling(staminaScaling);
        scaling.setVitalityScaling(vitalityScaling);
        scaling.setKiPowerScaling(kiPowerScaling);
        scaling.setEnergyScaling(energyScaling);
        classStats.setStatScaling(scaling);
        classStats.setBaseHp5(0.0D);
        classStats.setBaseEp5(0.0D);
        classStats.setBaseSp5(0.0D);
        classStats.setHp5VitScaling(0.0D);
        classStats.setEp5EneScaling(0.0D);
        classStats.setSp5StmScaling(0.0D);
        classStats.setTpCostMultiplier(0.0D);
        classStats.setTpGainMultiplier(0.0D);
        classStats.setPassive(defaultPassive(""));
        return classStats;
    }

    private static RaceStatsConfig.ClassStats withTp(RaceStatsConfig.ClassStats classStats, double tpCostMultiplier, double tpGainMultiplier) {
        classStats.setTpCostMultiplier(tpCostMultiplier);
        classStats.setTpGainMultiplier(tpGainMultiplier);
        return classStats;
    }

    // Creates the built-in passive config values used by the current DMZ beta.
    private static RaceStatsConfig.Passive defaultPassive(String classId) {
        RaceStatsConfig.Passive passive = new RaceStatsConfig.Passive();
        passive.setEnabled(true);
        passive.setValues(switch (normalize(classId)) {
            case "warrior" -> Map.of(
                    "maxStacks", 10.0D,
                    "staminaRegenPerStack", 0.05D,
                    "defensePenetrationPerStack", 0.01D,
                    "stackDurationTicks", 100.0D
            );
            case "berserker" -> Map.of(
                    "critChancePerMissingHpPercent", 0.005D,
                    "critDamagePerMissingHpPercent", 0.01D
            );
            case "spiritualist" -> Map.of(
                    "costReduction", 0.20D,
                    "cooldownReduction", 0.20D,
                    "effectCooldownReduction", 0.15D,
                    "effectDurationBonus", 0.25D
            );
            case "tank" -> Map.of(
                    "stmToHpRegenRatio", 0.50D,
                    "healingBonus", 0.25D,
                    "lowHpThreshold", 0.30D,
                    "lowHpMultiplier", 2.0D
            );
            case "cleric" -> Map.of(
                    "costReduction", 0.20D,
                    "cooldownReduction", 0.20D,
                    "effectCooldownReduction", 0.15D,
                    "effectDurationBonus", 0.25D
            );
            case "speedster" -> Map.of(
                    "maxStacks", 10.0D,
                    "speedBonusPerStack", 0.01D,
                    "meleeDamageSpeedSharePerStack", 0.05D,
                    "stackDurationTicks", 200.0D
            );
            case "martialartist" -> Map.of(
                    "targetHpThreshold", 0.50D,
                    "damageBonus", 0.25D
            );
            case "duelist" -> Map.of(
                    "parryPoiseDamageBonus", 0.10D,
                    "guardBrokenDamageBonus", 0.50D,
                    "guardBrokenKnockbackBonus", 1.0D,
                    "kiParrySpeedBonus", 0.20D
            );
            case "kiassassin" -> Map.of(
                    "noEffectCastTimeReduction", 0.50D,
                    "noEffectSpeedIncrease", 0.50D,
                    "effectCastTimeReduction", 0.20D,
                    "effectSpeedIncrease", 0.20D
            );
            case "paladin" -> Map.of(
                    "redirectPct", 0.15D,
                    "lifestealPct", 0.15D
            );
            case "potentialist" -> Map.of(
                    "Custom Passive", 1.0D,
                    "PassiveType", 4.0D,
                    "Type", 6.0D,
                    "Value", 0.5D
            );
            default -> Map.of();
        });
        return passive;
    }

    // Adds integer race and class values.
    private static Integer sum(Integer raceValue, Integer classValue) {
        return value(raceValue) + value(classValue);
    }

    // Adds decimal race and class values.
    private static Double sum(Double raceValue, Double classValue) {
        return value(raceValue) + value(classValue);
    }

    // Adds decimal values while preserving null when both sides are absent.
    private static Double sumNullable(Double raceValue, Double classValue) {
        return raceValue == null && classValue == null ? null : sum(raceValue, classValue);
    }

    // Returns zero when an integer value is null.
    private static int value(Integer value) {
        return value != null ? value : 0;
    }

    // Returns zero when a decimal value is null.
    private static double value(Double value) {
        return value != null ? value : 0.0D;
    }

    // Normalizes ids from files, race configs, and class configs.
    private static String normalize(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT).replace("_", "").replace(" ", "");
    }

    // Creates a readable display name from an id.
    private static String titleCase(String id) {
        String normalized = id == null ? "" : id.trim().toLowerCase(Locale.ROOT).replace('_', ' ');
        if (normalized.isEmpty()) {
            return "Custom Class";
        }

        StringBuilder builder = new StringBuilder();
        for (String part : normalized.split(" ")) {
            if (part.isEmpty()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.isEmpty() ? "Custom Class" : builder.toString();
    }

    // Saves a complete class config with stats and metadata.
    private static void saveClassConfig(Path path, RaceStatsConfig.ClassStats stats, ClassMetadata metadata) throws IOException {
        JsonObject json = GSON.toJsonTree(stats).getAsJsonObject();
        if (json.has("passive") && json.get("passive").isJsonObject()) {
            JsonObject passive = json.getAsJsonObject("passive");
            if (passive.has("values") && passive.get("values").isJsonObject()) {
                JsonObject values = passive.getAsJsonObject("values");
                if (values.has("Custom Passive")) {
                    values.addProperty("Custom Passive", values.get("Custom Passive").getAsDouble() != 0D);
                }
                for (String integerKey : List.of("PassiveType", "Type", "MaxStacks", "StackTime", "ResourceType", "Effect")) {
                    if (values.has(integerKey) && values.get(integerKey).isJsonPrimitive()
                            && values.get(integerKey).getAsJsonPrimitive().isNumber()) {
                        values.addProperty(integerKey, (int) Math.round(values.get(integerKey).getAsDouble()));
                    }
                }
            }
        }
        json.addProperty(CONFIG_VERSION_KEY, RaceStatsConfig.CURRENT_VERSION);
        json.addProperty(DISPLAY_NAME_KEY, metadata.displayName());
        json.addProperty(DISPLAY_COLOR_KEY, displayColorAsHex(metadata.displayColor()));
        json.add(EXCLUSIVE_RACES_KEY, toJsonArray(metadata.exclusiveRaces()));
        saveJson(path, json);
    }

    private static void writeCustomPassiveTutorial() throws IOException {
        Path path = CLASSES_DIR.resolve(CUSTOM_PASSIVE_TUTORIAL);
        Files.writeString(path, CustomClassPassives.TUTORIAL, StandardCharsets.UTF_8);
    }

    private static boolean isNamedDisplayColor(String value) {
        return value != null && !value.isBlank() && !value.startsWith("#")
                && !value.startsWith("0x") && !value.startsWith("0X");
    }

    private static boolean hasDecimalCustomPassiveEnums(JsonObject json) {
        if (json == null || !json.has("passive") || !json.get("passive").isJsonObject()) return false;
        JsonObject passive = json.getAsJsonObject("passive");
        if (!passive.has("values") || !passive.get("values").isJsonObject()) return false;
        JsonObject values = passive.getAsJsonObject("values");
        for (String integerKey : List.of("PassiveType", "Type", "MaxStacks", "StackTime", "ResourceType", "Effect")) {
            JsonElement element = values.get(integerKey);
            if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()
                    && element.toString().contains(".")) return true;
        }
        return false;
    }

    private static String displayColorAsHex(String value) {
        if (!isNamedDisplayColor(value)) return value == null || value.isBlank() ? "#FFFFFF" : value;
        ChatFormatting formatting = ChatFormatting.getByName(value.toLowerCase(Locale.ROOT));
        Integer rgb = formatting != null ? formatting.getColor() : null;
        return String.format(Locale.ROOT, "#%06X", rgb != null ? rgb & 0xFFFFFF : 0xFFFFFF);
    }

    // Saves a JSON object to disk.
    private static void saveJson(Path path, JsonObject json) throws IOException {
        Files.createDirectories(path.getParent());
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(json, writer);
        }
    }

    public record ClassMetadata(String classId, String displayName, String displayColor, List<String> exclusiveRaces) {
        // Returns the configured color as an RGB integer.
        public int colorValue() {
            String value = displayColor == null ? "" : displayColor.trim();
            if (value.startsWith("#")) {
                try {
                    return Integer.parseInt(value.substring(1), 16) & 0xFFFFFF;
                } catch (NumberFormatException ignored) {
                    return 0xFFFFFF;
                }
            }
            if (value.startsWith("0x") || value.startsWith("0X")) {
                try {
                    return Integer.parseInt(value.substring(2), 16) & 0xFFFFFF;
                } catch (NumberFormatException ignored) {
                    return 0xFFFFFF;
                }
            }

            ChatFormatting formatting = ChatFormatting.getByName(value.toLowerCase(Locale.ROOT));
            Integer color = formatting != null ? formatting.getColor() : null;
            return color != null ? color : 0xFFFFFF;
        }
    }

    private record ClassConfigEntry(RaceStatsConfig.ClassStats stats, ClassMetadata metadata) {
    }
}
