package com.dmzrevamp.compat.sdu.client;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.shurui.dev.sdu.client.gui.FieldEditScreen;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class OverhaulQuestOptionsScreen extends FieldEditScreen {
    private final JsonObject options;
    public OverhaulQuestOptionsScreen(net.minecraft.client.gui.screens.Screen parent, JsonObject options) {
        super(Component.literal("Overhaul Options"), 300, 260, parent); this.options = options;
    }
    @Override protected void init() {
        super.init(); clearFields();
        rowY = buildTabHeader("Overhaul Options", new String[]{"Stats & Effect"}, 0, ignored -> {});
        number("Armor", "Armor"); number("Armor Toughness", "ArmorToughness"); number("Protection Points", "Protection");
        JsonObject effect = effect();
        List<String> effects = new ArrayList<>(); effects.add("none");
        BuiltInRegistries.MOB_EFFECT.keySet().stream().map(ResourceLocation::toString).sorted(Comparator.naturalOrder()).forEach(effects::add);
        df("Mob Effect", effects, effect == null ? "none" : string(effect, "effectId", "none"), value -> {
            if ("none".equals(value)) options.remove("mobEffect");
            else { JsonObject target = effect(); if (target == null) target = defaultEffect(); target.addProperty("effectId", value); options.add("mobEffect", target); }
            Minecraft.getInstance().execute(this::rebuildWidgets);
        });
        effect = effect();
        if (effect != null) {
            JsonObject target = effect;
            tf("Amplifier", Integer.toString(integer(target, "amplifier", 0)), value -> target.addProperty("amplifier", parseI(value, integer(target, "amplifier", 0))));
            tf("Duration Ticks", Integer.toString(integer(target, "durationTicks", -1)), value -> target.addProperty("durationTicks", parseI(value, integer(target, "durationTicks", -1))));
            bf("Visible", bool(target, "visible", false), () -> { target.addProperty("visible", !bool(target, "visible", false)); Minecraft.getInstance().execute(this::rebuildWidgets); });
        }
        btn(95, footerY(), 110, footerBtnHeight(), Component.literal("Back"), () -> { applyFields(); back(); });
    }
    private void number(String label, String key) { tf(label, options.has(key) ? options.get(key).getAsString() : "0", value -> { try { options.addProperty(key, Double.parseDouble(value.trim())); } catch (RuntimeException ignored) {} }); }
    private JsonObject effect() { return options.has("mobEffect") && options.get("mobEffect").isJsonObject() ? options.getAsJsonObject("mobEffect") : null; }
    private static JsonObject defaultEffect() { JsonObject o = new JsonObject(); o.addProperty("amplifier", 0); o.addProperty("durationTicks", -1); o.addProperty("visible", false); return o; }
    private static String string(JsonObject o, String k, String f) { try { return o.get(k).getAsString(); } catch (RuntimeException e) { return f; } }
    private static int integer(JsonObject o, String k, int f) { try { return o.get(k).getAsInt(); } catch (RuntimeException e) { return f; } }
    private static boolean bool(JsonObject o, String k, boolean f) { try { return o.get(k).getAsBoolean(); } catch (RuntimeException e) { return f; } }
}
