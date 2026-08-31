package com.dmzrevamp.compat.sdu.client;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.shurui.dev.sdu.client.gui.FieldEditScreen;

import java.util.List;

public final class SduCustomPassiveScreen extends FieldEditScreen {
    private static final String HELP_TEXT = "for more information on how to set a class passive, go to config\\dragonminez\\classes and read the .txt file!";
    private static final List<String> PASSIVE_TYPES = List.of("1", "2", "3", "4");

    private final JsonObject passive;
    private final JsonObject values;

    public SduCustomPassiveScreen(Screen parent, JsonObject passive) {
        super(Component.literal("Custom Passive"), 300, 260, parent);
        this.passive = passive == null ? new JsonObject() : passive;
        if (!this.passive.has("enabled")) {
            this.passive.addProperty("enabled", true);
        }
        if (!this.passive.has("values") || !this.passive.get("values").isJsonObject()) {
            this.passive.add("values", new JsonObject());
        }
        this.values = this.passive.getAsJsonObject("values");
    }

    @Override
    protected void init() {
        super.init();
        clearFields();
        rowY = buildTabHeader("Custom Passive", new String[]{"Custom Passive"}, 0, ignored -> {
        });

        bf("Custom Passive", boolValue("Custom Passive", false), () -> {
            values.addProperty("Custom Passive", !boolValue("Custom Passive", false));
            rebuildSoon();
        });
        df("Passive Type", PASSIVE_TYPES, Integer.toString(intValue("PassiveType", 1)), selected -> {
            values.addProperty("PassiveType", parseI(selected, 1));
            rebuildSoon();
        });

        switch (intValue("PassiveType", 1)) {
            case 1 -> buildSimple();
            case 2 -> buildCombo();
            case 3 -> buildResource();
            case 4 -> buildSpecial();
            default -> values.addProperty("PassiveType", 1);
        }

        btn(95, 238, 110, 16, Component.literal("Back"), () -> {
            applyFields();
            back();
        });
    }

    private void buildSimple() {
        integerField("Effect", 1);
        decimalField("Value", 0D);
    }

    private void buildCombo() {
        integerField("Type", 1);
        integerField("Max Stacks", "MaxStacks", 1);
        integerField("Stack Time", "StackTime", 20);
        integerField("Effect", 1);
        decimalField("Max Value", "MaxValue", 0D);
    }

    private void buildResource() {
        integerField("Type", 1);
        integerField("Effect", 1);
        decimalField("Value", 0D);
        integerField("Resource Type", "ResourceType", 1);
    }

    private void buildSpecial() {
        integerField("Type", 1);
        decimalField("Value", 0D);
    }

    private void integerField(String key, int fallback) {
        integerField(key, key, fallback);
    }

    private void integerField(String label, String key, int fallback) {
        tf(label, Integer.toString(intValue(key, fallback)), text ->
                values.addProperty(key, parseI(text, intValue(key, fallback))));
    }

    private void decimalField(String key, double fallback) {
        decimalField(key, key, fallback);
    }

    private void decimalField(String label, String key, double fallback) {
        tf(label, dbl(doubleValue(key, fallback)), text ->
                values.addProperty(key, parseD(text, doubleValue(key, fallback))));
    }

    private boolean boolValue(String key, boolean fallback) {
        try {
            if (!values.has(key)) {
                return fallback;
            }
            if (values.get(key).isJsonPrimitive() && values.getAsJsonPrimitive(key).isNumber()) {
                return values.get(key).getAsDouble() != 0D;
            }
            return values.get(key).getAsBoolean();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private int intValue(String key, int fallback) {
        try {
            return values.has(key) ? values.get(key).getAsInt() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private double doubleValue(String key, double fallback) {
        try {
            return values.has(key) ? values.get(key).getAsDouble() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private void rebuildSoon() {
        Minecraft.getInstance().execute(this::rebuildWidgets);
    }

    @Override
    protected void renderTopOverlay(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderTopOverlay(graphics, mouseX, mouseY);
        graphics.pose().pushPose();
        graphics.pose().scale(0.5F, 0.5F, 1F);
        int x = (600 - font.width(HELP_TEXT)) / 2;
        graphics.drawString(font, HELP_TEXT, x, 442, 0xFFD0D0D0, false);
        graphics.pose().popPose();
    }
}
