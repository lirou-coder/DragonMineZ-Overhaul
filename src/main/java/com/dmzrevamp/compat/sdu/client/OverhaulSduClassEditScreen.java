package com.dmzrevamp.compat.sdu.client;

import com.dmzrevamp.mixin.compat.sdu.SduRaceClassStatsAccessor;
import com.dmzrevamp.network.DmzRevampNetwork;
import com.dmzrevamp.network.SaveSduClassC2SPacket;
import com.dmzrevamp.revamp.classes.DmzClassConfigManager;
import com.dragonminez.common.config.RaceStatsConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.shurui.dev.sdu.client.gui.race.RaceClassStatsScreen;
import net.shurui.dev.sdu.race.RaceData;

import java.util.Locale;

public final class OverhaulSduClassEditScreen extends RaceClassStatsScreen {
    private static final Gson GSON = new Gson();

    private final String originalClassId;
    private final boolean newClass;
    private final JsonObject originalJson;
    private String displayName;
    private String displayColor;

    public OverhaulSduClassEditScreen(Screen parent, String classId, String json, boolean newClass) {
        this(parent, build(classId, json), classId, newClass);
    }

    private OverhaulSduClassEditScreen(Screen parent, EditorData editorData, String classId, boolean newClass) {
        super(parent, editorData.race(), classId);
        this.originalClassId = classId;
        this.newClass = newClass;
        this.originalJson = editorData.json();
        this.displayName = newClass ? "New Class" : readString(editorData.json(), "displayName", "New Class");
        this.displayColor = newClass ? "#FFFFFF" : readString(editorData.json(), "displayColor", "#FFFFFF");
    }

    @Override
    protected void init() {
        super.init();
        SduRaceClassStatsAccessor access = (SduRaceClassStatsAccessor) (Object) this;
        String effectiveId = newClass ? access.dmzrevamp$className() : originalClassId;
        if (access.dmzrevamp$section() == 2) {
            tf("Display Name", displayName, value -> displayName = value);
            tf("Display Color", displayColor, value -> displayColor = value);
        }
        if (access.dmzrevamp$section() == 2 && DmzClassConfigManager.canEditCustomPassive(effectiveId)) {
            btn(95, Math.min(rowY, 278), 110, 16, Component.literal("Custom Passive"), () -> {
                applyFields();
                Minecraft.getInstance().setScreen(new SduCustomPassiveScreen(this,
                        access.dmzrevamp$classStats().passive));
            });
        }
    }

    @Override
    protected void back() {
        applyFields();
        SduRaceClassStatsAccessor access = (SduRaceClassStatsAccessor) (Object) this;
        String classId = newClass ? normalizeId(access.dmzrevamp$className()) : normalizeId(originalClassId);
        if (!classId.isEmpty() && !"race".equals(classId)) {
            JsonObject json = toOverhaulJson(access.dmzrevamp$classStats(), classId);
            DmzRevampNetwork.CHANNEL.sendToServer(new SaveSduClassC2SPacket(classId, GSON.toJson(json)));
        }
        super.back();
    }

    private JsonObject toOverhaulJson(RaceData.ClassStats source, String classId) {
        RaceStatsConfig.ClassStats target = new RaceStatsConfig.ClassStats();
        RaceStatsConfig.BaseStats base = new RaceStatsConfig.BaseStats();
        base.setStrength(source.str);
        base.setStrikePower(source.skp);
        base.setResistance(source.res);
        base.setVitality(source.vit);
        base.setKiPower(source.pwr);
        base.setEnergy(source.ene);
        target.setBaseStats(base);

        RaceStatsConfig.StatScaling scaling = new RaceStatsConfig.StatScaling();
        scaling.setStrengthScaling(source.strScaling);
        scaling.setStrikePowerScaling(source.skpScaling);
        scaling.setStaminaScaling(source.stmScaling);
        scaling.setDefenseScaling(source.defScaling);
        scaling.setVitalityScaling(source.vitScaling);
        scaling.setKiPowerScaling(source.pwrScaling);
        scaling.setEnergyScaling(source.eneScaling);
        target.setStatScaling(scaling);
        target.setBaseHp5(source.baseHp5);
        target.setHp5VitScaling(source.hp5VitScaling);
        target.setBaseEp5(source.baseEp5);
        target.setEp5EneScaling(source.ep5EneScaling);
        target.setBaseSp5(source.baseSp5);
        target.setSp5StmScaling(source.sp5StmScaling);
        target.setTpCostMultiplier(source.tpCostMultiplier);
        target.setTpGainMultiplier(source.tpGainMultiplier);

        JsonObject result = GSON.toJsonTree(target).getAsJsonObject();
        copyMetadata("configVersion", result);
        copyMetadata("exclusiveRaces", result);
        result.addProperty("displayName", displayName == null || displayName.isBlank() ? "New Class" : displayName.trim());
        result.addProperty("displayColor", displayColor == null || displayColor.isBlank() ? "#FFFFFF" : displayColor.trim());
        result.add("passive", source.passive == null ? defaultPassive() : source.passive.deepCopy());
        return result;
    }

    private void copyMetadata(String key, JsonObject result) {
        if (originalJson.has(key)) {
            result.add(key, originalJson.get(key).deepCopy());
        }
    }

    private static EditorData build(String classId, String jsonText) {
        JsonObject json;
        try {
            json = JsonParser.parseString(jsonText).getAsJsonObject();
        } catch (RuntimeException ignored) {
            json = new JsonObject();
        }
        RaceStatsConfig.ClassStats source = GSON.fromJson(json, RaceStatsConfig.ClassStats.class);
        RaceData.ClassStats target = new RaceData.ClassStats();
        if (source != null) {
            RaceStatsConfig.BaseStats base = source.getBaseStats();
            RaceStatsConfig.StatScaling scaling = source.getStatScaling();
            if (base != null) {
                target.str = value(base.getStrength());
                target.skp = value(base.getStrikePower());
                target.res = value(base.getResistance());
                target.vit = value(base.getVitality());
                target.pwr = value(base.getKiPower());
                target.ene = value(base.getEnergy());
            }
            if (scaling != null) {
                target.strScaling = value(scaling.getStrengthScaling());
                target.skpScaling = value(scaling.getStrikePowerScaling());
                target.stmScaling = value(scaling.getStaminaScaling());
                target.defScaling = value(scaling.getDefenseScaling());
                target.vitScaling = value(scaling.getVitalityScaling());
                target.pwrScaling = value(scaling.getKiPowerScaling());
                target.eneScaling = value(scaling.getEnergyScaling());
            }
            target.baseHp5 = value(source.getBaseHp5());
            target.hp5VitScaling = value(source.getHp5VitScaling());
            target.baseEp5 = value(source.getBaseEp5());
            target.ep5EneScaling = value(source.getEp5EneScaling());
            target.baseSp5 = value(source.getBaseSp5());
            target.sp5StmScaling = value(source.getSp5StmScaling());
            target.tpCostMultiplier = value(source.getTpCostMultiplier());
            target.tpGainMultiplier = value(source.getTpGainMultiplier());
        }
        target.passive = json.has("passive") && json.get("passive").isJsonObject()
                ? json.getAsJsonObject("passive").deepCopy() : defaultPassive();

        RaceData race = new RaceData();
        race.classes.put(classId, target);
        return new EditorData(race, json);
    }

    private static JsonObject defaultPassive() {
        JsonObject passive = new JsonObject();
        passive.addProperty("enabled", true);
        passive.add("values", new JsonObject());
        return passive;
    }

    private static int value(Integer number) {
        return number == null ? 0 : number;
    }

    private static double value(Double number) {
        return number == null ? 0D : number;
    }

    private static String normalizeId(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT).replace(" ", "").replace("_", "");
    }

    private static String readString(JsonObject json, String key, String fallback) {
        try {
            if (json != null && json.has(key) && json.get(key).isJsonPrimitive()) {
                String value = json.get(key).getAsString();
                if (!value.isBlank()) {
                    return value;
                }
            }
        } catch (RuntimeException ignored) {
        }
        return fallback;
    }

    private record EditorData(RaceData race, JsonObject json) {
    }
}
