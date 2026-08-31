package com.dmzrevamp.revamp.defaults;

import com.dmzrevamp.compat.DmzSparkingCompat;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.quest.QuestUpgrader;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PrestigeSagaDefaults {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String ROOT = "data/dmzrevamp/defaults/";
    private static final String QUEST_FOLDER = "saga_prestige";
    private static final String[] QUEST_FILES = {
            "01_path_to_greater_power.json",
            "02_breaking_your_limits.json",
            "03_may_your_power_reborn.json"
    };

    private PrestigeSagaDefaults() {
    }

    public static void writeQuestDefaults(Path questsPath) {
        if (!shouldCreateDefaults()) {
            return;
        }

        Path dmzRoot = questsPath.getParent();
        Path prestigeFolder = questsPath.resolve(QUEST_FOLDER);
        for (String fileName : QUEST_FILES) {
            writeDefault(dmzRoot, prestigeFolder.resolve(fileName),
                    ROOT + "quests/" + QUEST_FOLDER + "/" + fileName);
        }
    }

    public static void writeAllDefaults(Path questsPath) {
        writeQuestDefaults(questsPath);
        writeSagaDefault(questsPath.getParent().resolve("sagas"));
    }

    public static void writeSagaDefault(Path sagasPath) {
        if (!shouldCreateDefaults()) {
            return;
        }

        writeDefault(sagasPath.getParent(), sagasPath.resolve("prestige_saga.json"),
                ROOT + "sagas/prestige_saga.json");
    }

    private static boolean shouldCreateDefaults() {
        return !DmzSparkingCompat.isLoaded()
                && Boolean.TRUE.equals(ConfigManager.getServerConfig().getGameplay().getStoryModeEnabled())
                && Boolean.TRUE.equals(ConfigManager.getServerConfig().getGameplay().getCreateDefaultSagas());
    }

    private static void writeDefault(Path dmzRoot, Path target, String resourcePath) {
        try (InputStream stream = PrestigeSagaDefaults.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                LOGGER.error("Missing bundled Prestige saga default: {}", resourcePath);
                return;
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject defaultJson = JsonParser.parseReader(reader).getAsJsonObject();
                Files.createDirectories(target.getParent());
                QuestUpgrader.upgradeOrWrite(dmzRoot, target, defaultJson);
            }
        } catch (Exception exception) {
            LOGGER.error("Failed to create Prestige saga default {}", target, exception);
        }
    }
}
