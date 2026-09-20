package com.dmzrevamp.compat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.dragonminez.common.config.DefaultFormsFactory;
import com.dragonminez.common.config.SkillsConfig;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/** Seeds complete Overhaul/DMZ documents before Noea's additive overlay installer runs. */
public final class NoeaConfigCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String ROOT = "data/dmzrevamp/defaults/";
    private static final String[] RACES = {"human", "saiyan", "namekian", "frostdemon", "bioandroid", "majin"};

    private NoeaConfigCompat() {
    }

    public static void prepareBaseDefaults() {
        if (!NoeaCompat.isLoadedEarly()) {
            return;
        }
        Path dmz = FMLPaths.CONFIGDIR.get().resolve("dragonminez");
        seedSkills(dmz.resolve("skills.json"));
        seedCompleteDmzForms(dmz);
        for (String race : RACES) {
            mergeMissingResource(ROOT + "races/" + race + "/character.json",
                    dmz.resolve("races").resolve(race).resolve("character.json"));
        }
        try (InputStream stream = NoeaConfigCompat.class.getClassLoader()
                .getResourceAsStream(ROOT + "forms/index.txt")) {
            if (stream == null) return;
            String index = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            for (String entry : index.lines().map(String::trim).filter(line -> line.endsWith(".json")).toList()) {
                Path target = entry.startsWith("races/")
                        ? dmz.resolve(entry)
                        : dmz.resolve("forms").resolve(Path.of(entry).getFileName());
                mergeOverhaulSnapshot(ROOT + "forms/" + entry, target);
            }
        } catch (Exception exception) {
            LOGGER.warn("Could not prepare Overhaul form defaults before Noea overlays: {}", exception.getMessage());
        }
    }

    private static void mergeMissingResource(String resource, Path target) {
        try (InputStream stream = NoeaConfigCompat.class.getClassLoader().getResourceAsStream(resource)) {
            if (stream == null) return;
            JsonObject base = JsonParser.parseString(new String(stream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            if (!Files.exists(target)) {
                Files.createDirectories(target.getParent());
                Files.writeString(target, GSON.toJson(base), StandardCharsets.UTF_8);
                return;
            }
            JsonObject current = JsonParser.parseString(Files.readString(target, StandardCharsets.UTF_8)).getAsJsonObject();
            if (mergeMissing(current, base)) Files.writeString(target, GSON.toJson(current), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            LOGGER.warn("Could not merge complete character defaults into {}: {}", target, exception.getMessage());
        }
    }

    private static void seedSkills(Path target) {
        try {
            Files.createDirectories(target.getParent());
            JsonObject base = GSON.toJsonTree(new SkillsConfig()).getAsJsonObject();
            if (!Files.exists(target)) {
                Files.writeString(target, GSON.toJson(base), StandardCharsets.UTF_8);
                return;
            }
            JsonObject current = JsonParser.parseString(Files.readString(target, StandardCharsets.UTF_8)).getAsJsonObject();
            if (mergeMissing(current, base)) {
                Files.writeString(target, GSON.toJson(current), StandardCharsets.UTF_8);
            }
        } catch (Exception exception) {
            LOGGER.warn("Could not seed complete DMZ skills before Noea overlays: {}", exception.getMessage());
        }
    }

    private static void seedCompleteDmzForms(Path dmz) {
        Path temporary = null;
        try {
            temporary = Files.createTempDirectory("dmzrevamp-noea-base-");
            DefaultFormsFactory factory = new DefaultFormsFactory();
            for (String race : RACES) {
                Path forms = temporary.resolve("races").resolve(race).resolve("forms");
                Files.createDirectories(forms);
                factory.createDefaultFormsForRace(race, forms, new HashMap<>());
            }
            Path stack = temporary.resolve("forms");
            Files.createDirectories(stack);
            factory.createDefaultStackForms(stack, new HashMap<>());

            try (var paths = Files.walk(temporary)) {
                for (Path source : paths.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".json")).toList()) {
                    Path target = dmz.resolve(temporary.relativize(source));
                    mergeMissingDocument(source, target);
                }
            }
        } catch (Exception exception) {
            LOGGER.warn("Could not prepare the complete DMZ form baseline before Noea: {}", exception.getMessage());
        } finally {
            if (temporary != null) {
                try (var paths = Files.walk(temporary)) {
                    for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static void mergeMissingDocument(Path baseFile, Path target) throws Exception {
        if (!Files.exists(target)) {
            Files.createDirectories(target.getParent());
            Files.copy(baseFile, target, StandardCopyOption.REPLACE_EXISTING);
            return;
        }
        JsonObject current = JsonParser.parseString(Files.readString(target, StandardCharsets.UTF_8)).getAsJsonObject();
        JsonObject base = JsonParser.parseString(Files.readString(baseFile, StandardCharsets.UTF_8)).getAsJsonObject();
        if (mergeMissing(current, base)) {
            Files.writeString(target, GSON.toJson(current), StandardCharsets.UTF_8);
        }
    }

    private static void mergeOverhaulSnapshot(String resource, Path target) throws Exception {
        try (InputStream stream = NoeaConfigCompat.class.getClassLoader().getResourceAsStream(resource)) {
            if (stream == null) return;
            JsonObject overhaul = JsonParser.parseString(new String(stream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject current = Files.exists(target)
                    ? JsonParser.parseString(Files.readString(target, StandardCharsets.UTF_8)).getAsJsonObject()
                    : new JsonObject();
            mergeOverhaulKnown(current, overhaul);
            Files.createDirectories(target.getParent());
            Files.writeString(target, GSON.toJson(current), StandardCharsets.UTF_8);
        }
    }

    private static boolean mergeMissing(JsonObject target, JsonObject base) {
        boolean changed = false;
        for (Map.Entry<String, JsonElement> entry : base.entrySet()) {
            JsonElement existing = target.get(entry.getKey());
            if (existing == null) {
                target.add(entry.getKey(), entry.getValue().deepCopy());
                changed = true;
            } else if (existing.isJsonObject() && entry.getValue().isJsonObject()) {
                changed |= mergeMissing(existing.getAsJsonObject(), entry.getValue().getAsJsonObject());
            }
        }
        return changed;
    }

    private static void mergeOverhaulKnown(JsonObject target, JsonObject overhaul) {
        for (Map.Entry<String, JsonElement> entry : overhaul.entrySet()) {
            JsonElement existing = target.get(entry.getKey());
            if (existing != null && existing.isJsonObject() && entry.getValue().isJsonObject()) {
                mergeOverhaulKnown(existing.getAsJsonObject(), entry.getValue().getAsJsonObject());
            } else {
                target.add(entry.getKey(), entry.getValue().deepCopy());
            }
        }
    }

}
