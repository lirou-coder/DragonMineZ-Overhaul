package com.dmzrevamp.revamp.defaults;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.dragonminez.common.config.FormConfig;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DefaultConfigSnapshots {
    private static final Gson GSON = new GsonBuilder().create();
    private static final String FORM_ROOT = "data/dmzrevamp/defaults/forms/";
    private static final String QUEST_ROOT = "data/dmzrevamp/defaults/quests/";
    private static final Map<String, FormConfig> FORM_DEFAULTS = loadFormDefaults();
    private static final Map<String, JsonObject> QUEST_DEFAULTS = loadQuestDefaults();

    private DefaultConfigSnapshots() {
    }

    public static void applyRaceFormDefaults(String race, Map<String, FormConfig> forms) {
        if (race == null || forms == null) {
            return;
        }
        String prefix = "races/" + race.toLowerCase() + "/forms/";
        applyFormDefaults(prefix, forms);
    }

    public static void applyStackFormDefaults(Map<String, FormConfig> forms) {
        if (forms != null) {
            applyFormDefaults("stack/", forms);
        }
    }

    public static JsonObject questDefault(String relativePath) {
        JsonObject object = QUEST_DEFAULTS.get(normalize(relativePath));
        return object == null ? null : object.deepCopy();
    }

    private static void applyFormDefaults(String prefix, Map<String, FormConfig> forms) {
        for (Map.Entry<String, FormConfig> entry : FORM_DEFAULTS.entrySet()) {
            if (!entry.getKey().startsWith(prefix)) {
                continue;
            }
            String fileName = entry.getKey().substring(prefix.length());
            if (fileName.endsWith(".json")) {
                fileName = fileName.substring(0, fileName.length() - ".json".length());
            }
            forms.put(fileName, copyFormConfig(entry.getValue()));
        }
    }

    private static Map<String, FormConfig> loadFormDefaults() {
        Map<String, FormConfig> defaults = new HashMap<>();
        for (String path : readIndex(FORM_ROOT)) {
            FormConfig config = readJson(FORM_ROOT + path, FormConfig.class);
            if (config != null) {
                defaults.put(normalize(path), config);
            }
        }
        return defaults;
    }

    private static Map<String, JsonObject> loadQuestDefaults() {
        Map<String, JsonObject> defaults = new HashMap<>();
        for (String path : readIndex(QUEST_ROOT)) {
            JsonObject object = readJson(QUEST_ROOT + path, JsonObject.class);
            if (object != null) {
                defaults.put(normalize(path), object);
            }
        }
        return defaults;
    }

    private static List<String> readIndex(String root) {
        try (InputStream stream = DefaultConfigSnapshots.class.getClassLoader().getResourceAsStream(root + "index.txt")) {
            if (stream == null) {
                return List.of();
            }
            String content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            return content.lines()
                    .map(DefaultConfigSnapshots::normalize)
                    .filter(line -> !line.isBlank())
                    .filter(line -> line.endsWith(".json"))
                    .toList();
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private static <T> T readJson(String resourcePath, Class<T> type) {
        try (InputStream stream = DefaultConfigSnapshots.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                return null;
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                if (type == JsonObject.class) {
                    return type.cast(JsonParser.parseReader(reader).getAsJsonObject());
                }
                return GSON.fromJson(reader, type);
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static FormConfig copyFormConfig(FormConfig config) {
        return GSON.fromJson(GSON.toJson(config), FormConfig.class);
    }

    private static String normalize(String path) {
        return path == null ? "" : path.replace('\\', '/').trim();
    }
}
