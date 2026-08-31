// Loads and saves the JSON files that let servers tune each Overhaul racial mechanic separately.
package com.dmzrevamp.config.racial;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DmzRevampRacialConfigs {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path ROOT_DIR = FMLPaths.CONFIGDIR.get().resolve("dmzrevamp").resolve("racials");
    private static BioAndroidRacialConfig bioAndroid;
    private static FrostDemonRacialConfig frostDemon;
    private static HumanRpgRacialConfig humanRpg;
    private static SaiyanRpgRacialConfig saiyanRpg;
    private static MajinRevampRacialConfig majinRevamp;
    private static NamekianRevampRacialConfig namekianRevamp;

    // All racial configs are shared singletons, so this class should not be instantiated.
    private DmzRevampRacialConfigs() {
    }

    // Reads every racial config file, creating missing files with defaults on first run.
    public static void loadAll() {
        bioAndroid = load("bioandroidrevamp.json", BioAndroidRacialConfig.class, new BioAndroidRacialConfig());
        frostDemon = load("frostrevamp.json", FrostDemonRacialConfig.class, new FrostDemonRacialConfig());
        humanRpg = load("humanrevamp.json", HumanRpgRacialConfig.class, new HumanRpgRacialConfig());
        saiyanRpg = load("saiyanrevamp.json", SaiyanRpgRacialConfig.class, new SaiyanRpgRacialConfig());
        majinRevamp = load("majinrevamp.json", MajinRevampRacialConfig.class, new MajinRevampRacialConfig());
        namekianRevamp = load("namekianrevamp.json", NamekianRevampRacialConfig.class, new NamekianRevampRacialConfig());
    }

    // Returns Bio Android settings, loading config files first if startup did not do it yet.
    public static BioAndroidRacialConfig bioAndroid() {
        if (bioAndroid == null) {
            loadAll();
        }
        return bioAndroid;
    }

    // Returns Frost Demon settings, loading config files first if startup did not do it yet.
    public static FrostDemonRacialConfig frostDemon() {
        if (frostDemon == null) {
            loadAll();
        }
        return frostDemon;
    }

    // Returns Human/Android settings, loading config files first if startup did not do it yet.
    public static HumanRpgRacialConfig humanRpg() {
        if (humanRpg == null) {
            loadAll();
        }
        return humanRpg;
    }

    // Returns Saiyan Zenkai settings, loading config files first if startup did not do it yet.
    public static SaiyanRpgRacialConfig saiyanRpg() {
        if (saiyanRpg == null) {
            loadAll();
        }
        return saiyanRpg;
    }

    public static MajinRevampRacialConfig majinRevamp() {
        if (majinRevamp == null) {
            loadAll();
        }
        return majinRevamp;
    }

    public static NamekianRevampRacialConfig namekianRevamp() {
        if (namekianRevamp == null) loadAll();
        return namekianRevamp;
    }


    // Reads one JSON config, rewrites it with current comment fields, and falls back to defaults if the file is broken.
    private static <T> T load(String fileName, Class<T> type, T defaults) {
        try {
            Files.createDirectories(ROOT_DIR);
            Path configPath = ROOT_DIR.resolve(fileName);
            if (Files.notExists(configPath)) {
                save(configPath, defaults);
                return defaults;
            }

            try (Reader reader = Files.newBufferedReader(configPath)) {
                JsonElement root = JsonParser.parseReader(reader);
                if (root == null || !root.isJsonObject()) {
                    save(configPath, defaults);
                    return defaults;
                }
                JsonObject source = root.getAsJsonObject();
                T loaded = GSON.fromJson(source, type);
                if (loaded == null) {
                    save(configPath, defaults);
                    return defaults;
                }
                if (loaded instanceof BioAndroidRacialConfig bioAndroidConfig) {
                    // Migrate the former Overhaul default so existing generated
                    // configs receive the same HP regeneration as other races.
                    if (source.has("healthRegenMultiplier")
                            && Math.abs(source.get("healthRegenMultiplier").getAsDouble() - 0.75D) < 0.0000001D) {
                        bioAndroidConfig.healthRegenMultiplier = 0.5D;
                    }
                    bioAndroidConfig._comments = BioAndroidRacialConfig.createComments();
                }
                if (loaded instanceof HumanRpgRacialConfig humanConfig) {
                    humanConfig._comments = HumanRpgRacialConfig.createComments();
                }
                if (loaded instanceof SaiyanRpgRacialConfig saiyanConfig) {
                    if (!source.has("shadowDummyGiveZenkai")) {
                        saiyanConfig.shadowDummyGiveZenkai = false;
                    }
                    if (!source.has("shadowDummyFriendlyFist")) {
                        saiyanConfig.shadowDummyFriendlyFist = true;
                    }
                    saiyanConfig._comments = SaiyanRpgRacialConfig.createComments();
                }
                if (loaded instanceof MajinRevampRacialConfig majinConfig) {
                    majinConfig._comments = MajinRevampRacialConfig.createComments();
                }
                if (loaded instanceof NamekianRevampRacialConfig namekianConfig) {
                    // assimilationAmount was the old limiter. Its presence marks
                    // a legacy generated file whose 0.25 decay must migrate to
                    // the shared Zenkai/Absorption default of 0.02.
                    if (source.has("assimilationAmount")
                            && (!source.has("effectDecayPerUse")
                            || Math.abs(source.get("effectDecayPerUse").getAsDouble() - 0.25D) < 0.0000001D)) {
                        namekianConfig.effectDecayPerUse = 0.02D;
                    }
                    namekianConfig._comments = NamekianRevampRacialConfig.createComments();
                }
                save(configPath, loaded);
                return loaded;
            }
        } catch (IOException | RuntimeException exception) {
            try {
                Files.createDirectories(ROOT_DIR);
                save(ROOT_DIR.resolve(fileName), defaults);
            } catch (IOException ignored) {
            }
            return defaults;
        }
    }

    // Writes a config object as readable JSON.
    private static void save(Path path, Object value) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(value, writer);
        }
    }
}
