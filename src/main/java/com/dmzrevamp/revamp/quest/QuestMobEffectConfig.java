package com.dmzrevamp.revamp.quest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;

public record QuestMobEffectConfig(String effectId, int amplifier, int durationTicks, boolean ambient, boolean visible, boolean showIcon) {
    public static List<QuestMobEffectConfig> parseList(JsonElement element) {
        if (element == null || element.isJsonNull()) return List.of();
        List<QuestMobEffectConfig> result = new ArrayList<>();
        if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(entry -> addParsed(result, entry));
        } else {
            addParsed(result, element);
        }
        return List.copyOf(result);
    }

    private static void addParsed(List<QuestMobEffectConfig> result, JsonElement element) {
        if (element != null && element.isJsonObject()) {
            QuestMobEffectConfig parsed = fromJson(element.getAsJsonObject());
            if (parsed != null) result.add(parsed);
        }
    }

    public static QuestMobEffectConfig fromJson(JsonObject object) {
        String id = stringValue(object, "effectId", stringValue(object, "effectID", ""));
        if (id.isBlank()) {
            return null;
        }
        return new QuestMobEffectConfig(
                id,
                intValue(object, "amplifier", 0),
                intValue(object, "durationTicks", -1),
                booleanValue(object, "ambient", false),
                booleanValue(object, "visible", false),
                booleanValue(object, "showIcon", true)
        );
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("effectId", effectId);
        tag.putInt("amplifier", amplifier);
        tag.putInt("durationTicks", durationTicks);
        tag.putBoolean("ambient", ambient);
        tag.putBoolean("visible", visible);
        tag.putBoolean("showIcon", showIcon);
        return tag;
    }

    public static QuestMobEffectConfig load(CompoundTag tag) {
        return new QuestMobEffectConfig(
                tag.getString("effectId"),
                tag.getInt("amplifier"),
                tag.contains("durationTicks") ? tag.getInt("durationTicks") : -1,
                tag.contains("ambient") && tag.getBoolean("ambient"),
                tag.contains("visible") && tag.getBoolean("visible"),
                !tag.contains("showIcon") || tag.getBoolean("showIcon")
        );
    }

    private static String stringValue(JsonObject object, String key, String fallback) {
        JsonElement element = object.get(key);
        return element == null || element.isJsonNull() ? fallback : element.getAsString();
    }

    private static int intValue(JsonObject object, String key, int fallback) {
        JsonElement element = object.get(key);
        return element == null || element.isJsonNull() ? fallback : element.getAsInt();
    }

    private static boolean booleanValue(JsonObject object, String key, boolean fallback) {
        JsonElement element = object.get(key);
        return element == null || element.isJsonNull() ? fallback : element.getAsBoolean();
    }
}
