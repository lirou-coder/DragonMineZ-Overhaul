package com.dmzrevamp.revamp.battlepower;

import com.dmzrevamp.revamp.quest.QuestMobEffectConfig;
import com.dmzrevamp.revamp.quest.RevampKillObjectiveData;
import com.dragonminez.common.quest.Quest;
import com.dragonminez.common.quest.QuestObjective;
import com.dragonminez.common.quest.objectives.KillObjective;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class QuestPreviewExtraStatsResolver {
    private static final String RESOURCE_ROOT = "data/dmzrevamp/defaults/quests/";

    private QuestPreviewExtraStatsResolver() {
    }

    static ExtraStats resolve(Quest quest, KillObjective objective, int killIndex) {
        ExtraStats stats = fromObjective(objective);
        if (stats.hasAnyConfiguredValue()) {
            return stats;
        }

        ExtraStats jsonStats = fromJson(quest, objective, killIndex);
        return jsonStats.hasAnyConfiguredValue() ? jsonStats : stats;
    }

    private static ExtraStats fromObjective(KillObjective objective) {
        ExtraStats stats = new ExtraStats();
        if (objective instanceof RevampKillObjectiveData data) {
            stats.armor = data.dmzrevamp$getArmor();
            stats.armorToughness = data.dmzrevamp$getArmorToughness();
            stats.protection = data.dmzrevamp$getProtection();
            stats.movementSpeed = data.dmzrevamp$getMovementSpeed();
            stats.mobEffects = data.dmzrevamp$getMobEffects();
        }
        return stats;
    }

    private static ExtraStats fromJson(Quest quest, KillObjective objective, int killIndex) {
        if (quest == null || objective == null) {
            return new ExtraStats();
        }

        for (JsonObject questJson : candidateQuestJsons(quest)) {
            JsonObject objectiveJson = findKillObjectiveJson(questJson, objective, killIndex);
            if (objectiveJson != null) {
                return fromObjectiveJson(objectiveJson);
            }
        }
        return new ExtraStats();
    }

    private static List<JsonObject> candidateQuestJsons(Quest quest) {
        List<JsonObject> quests = new ArrayList<>();
        String category = quest.getCategory();
        if (category == null || category.isBlank()) {
            return quests;
        }

        Path configCategory = FMLPaths.CONFIGDIR.get().resolve("dragonminez").resolve("quests").resolve(category);
        if (Files.isDirectory(configCategory)) {
            try (var stream = Files.list(configCategory)) {
                stream.filter(path -> path.getFileName().toString().endsWith(".json"))
                        .map(QuestPreviewExtraStatsResolver::readJson)
                        .filter(json -> matchesQuest(json, quest))
                        .forEach(quests::add);
            } catch (Exception ignored) {
            }
        }

        for (String path : readResourceIndex()) {
            if (!path.startsWith(category + "/")) {
                continue;
            }
            JsonObject json = readResourceJson(RESOURCE_ROOT + path);
            if (matchesQuest(json, quest)) {
                quests.add(json);
            }
        }
        return quests;
    }

    private static boolean matchesQuest(JsonObject json, Quest quest) {
        if (json == null || quest == null) {
            return false;
        }
        if (quest.getStringId() != null && hasString(json, "id", quest.getStringId())) {
            return true;
        }
        if (quest.getId() >= 0 && hasInt(json, "id", quest.getId())) {
            return true;
        }
        return hasString(json, "title", quest.getTitle());
    }

    private static JsonObject findKillObjectiveJson(JsonObject questJson, KillObjective objective, int killIndex) {
        if (questJson == null || !questJson.has("objectives") || !questJson.get("objectives").isJsonArray()) {
            return null;
        }
        JsonArray objectives = questJson.getAsJsonArray("objectives");
        int currentKillIndex = 0;
        for (JsonElement element : objectives) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject object = element.getAsJsonObject();
            if (!hasString(object, "type", "KILL")) {
                continue;
            }
            if (currentKillIndex == killIndex || sameKillObjective(object, objective)) {
                return object;
            }
            currentKillIndex++;
        }
        return null;
    }

    private static boolean sameKillObjective(JsonObject object, KillObjective objective) {
        return hasString(object, "entity", objective.getEntityId())
                && close(number(object, "health", null), objective.getHealth())
                && close(number(object, "meleeDamage", null), objective.getMeleeDamage())
                && close(number(object, "kiDamage", null), objective.getKiDamage());
    }

    private static ExtraStats fromObjectiveJson(JsonObject object) {
        ExtraStats stats = new ExtraStats();
        stats.armor = number(object, "Armor", "armor");
        stats.armorToughness = number(object, "ArmorToughness", "armorToughness");
        stats.protection = number(object, "Protection", "protection");
        stats.movementSpeed = number(object, "movementSpeed", "MovementSpeed");
        stats.mobEffects = mobEffects(object);
        return stats;
    }

    private static List<QuestMobEffectConfig> mobEffects(JsonObject object) {
        JsonElement element = object.has("mobEffects") ? object.get("mobEffects") : object.get("mobEffect");
        if (element == null || element.isJsonNull()) {
            return List.of();
        }
        List<QuestMobEffectConfig> effects = new ArrayList<>();
        if (element.isJsonArray()) {
            for (JsonElement entry : element.getAsJsonArray()) {
                addMobEffect(effects, entry);
            }
        } else {
            addMobEffect(effects, element);
        }
        return effects;
    }

    private static void addMobEffect(List<QuestMobEffectConfig> effects, JsonElement element) {
        if (element != null && element.isJsonObject()) {
            QuestMobEffectConfig config = QuestMobEffectConfig.fromJson(element.getAsJsonObject());
            if (config != null) {
                effects.add(config);
            }
        }
    }

    private static List<String> readResourceIndex() {
        try (InputStream stream = QuestPreviewExtraStatsResolver.class.getClassLoader().getResourceAsStream(RESOURCE_ROOT + "index.txt")) {
            if (stream == null) {
                return List.of();
            }
            String content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            return content.lines()
                    .map(line -> line.replace('\\', '/').trim())
                    .filter(line -> !line.isBlank())
                    .filter(line -> line.endsWith(".json"))
                    .toList();
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private static JsonObject readResourceJson(String resourcePath) {
        try (InputStream stream = QuestPreviewExtraStatsResolver.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                return null;
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static JsonObject readJson(Path path) {
        try {
            return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Double number(JsonObject object, String primaryKey, String fallbackKey) {
        if (object == null) {
            return null;
        }
        JsonElement element = object.has(primaryKey) ? object.get(primaryKey) : fallbackKey == null ? null : object.get(fallbackKey);
        if (element == null || element.isJsonNull()) {
            return null;
        }
        try {
            return element.getAsDouble();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean hasString(JsonObject object, String key, String expected) {
        if (object == null || expected == null || !object.has(key) || object.get(key).isJsonNull()) {
            return false;
        }
        return expected.equalsIgnoreCase(object.get(key).getAsString());
    }

    private static boolean hasInt(JsonObject object, String key, int expected) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return false;
        }
        try {
            return object.get(key).getAsInt() == expected;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean close(Double configured, double objectiveValue) {
        return configured == null || Math.abs(configured - objectiveValue) < 1.0E-6D;
    }

    public static final class ExtraStats {
        public Double armor;
        public Double armorToughness;
        public Double protection;
        public Double movementSpeed;
        public List<QuestMobEffectConfig> mobEffects = List.of();

        boolean hasAnyConfiguredValue() {
            return armor != null
                    || armorToughness != null
                    || protection != null
                    || movementSpeed != null
                    || (mobEffects != null && !mobEffects.isEmpty());
        }
    }
}
