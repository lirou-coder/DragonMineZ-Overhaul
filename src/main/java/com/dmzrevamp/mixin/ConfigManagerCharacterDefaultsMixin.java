package com.dmzrevamp.mixin;

import com.dragonminez.common.config.ConfigLoader;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Changes only freshly generated DMZ character files; existing files remain authoritative. */
@Mixin(value = ConfigManager.class, priority = 2000, remap = false)
public abstract class ConfigManagerCharacterDefaultsMixin {
    private static final Set<String> DMZREVAMP_BASE_RACES = Set.of(
            "human", "saiyan", "namekian", "frostdemon", "majin", "bioandroid"
    );
    private static final ThreadLocal<ArrayDeque<Boolean>> DMZREVAMP_GENERATING_CHARACTER =
            ThreadLocal.withInitial(ArrayDeque::new);
    private static final Set<String> DMZREVAMP_MISSING_BEFORE_ADDON_OVERLAYS = new HashSet<>();

    @Shadow @Final private static Map<String, RaceCharacterConfig> RACE_CHARACTER;
    @Shadow @Final private static Path RACES_DIR;
    @Shadow @Final private static ConfigLoader LOADER;

    /**
     * Addons such as Noea install their character overlays at initialize HEAD. Capture
     * the original disk state before their default-priority injectors run, so a file
     * created by an addon in the same initialization is still treated as newly generated.
     */
    @Inject(method = "initialize", at = @At("HEAD"))
    private static void dmzrevamp$rememberCharactersMissingBeforeAddonOverlays(CallbackInfo ci) {
        DMZREVAMP_MISSING_BEFORE_ADDON_OVERLAYS.clear();
        for (String race : DMZREVAMP_BASE_RACES) {
            if (Files.notExists(RACES_DIR.resolve(race).resolve("character.json"))) {
                DMZREVAMP_MISSING_BEFORE_ADDON_OVERLAYS.add(race);
            }
        }
    }

    @Inject(method = "createOrLoadRace", at = @At("HEAD"))
    private static void dmzrevamp$rememberCharacterGeneration(String raceName, boolean defaultRace, CallbackInfo ci) {
        String race = dmzrevamp$normalizeRace(raceName);
        Path characterPath = RACES_DIR.resolve(race).resolve("character.json");
        boolean missingBeforeOverlays = DMZREVAMP_MISSING_BEFORE_ADDON_OVERLAYS.remove(race);
        DMZREVAMP_GENERATING_CHARACTER.get().push(missingBeforeOverlays || Files.notExists(characterPath));
    }

    @Inject(method = "createOrLoadRace", at = @At("TAIL"))
    private static void dmzrevamp$applyGeneratedCharacterDefaults(String raceName, boolean defaultRace, CallbackInfo ci) {
        ArrayDeque<Boolean> generationStack = DMZREVAMP_GENERATING_CHARACTER.get();
        boolean generated = !generationStack.isEmpty() && generationStack.pop();
        if (generationStack.isEmpty()) DMZREVAMP_GENERATING_CHARACTER.remove();
        if (!generated || !defaultRace) return;

        String race = dmzrevamp$normalizeRace(raceName);
        if (!DMZREVAMP_BASE_RACES.contains(race)) return;

        RaceCharacterConfig character = RACE_CHARACTER.get(race);
        if (character == null) return;

        // Racial is the one protected Overhaul field. Addon overlays may contribute every
        // other character value, but cannot replace the Revamp racial during generation.
        character.setRacialSkill(race + "revamp");

        // Emulate "Overhaul defaults first, addon overlay second": only replace DMZ's
        // untouched native Android costs. An addon-provided cost table remains authoritative.
        if ("human".equals(race) && Arrays.equals(
                character.getFormSkillTpCosts("androidforms"), new Integer[]{42_000, 104_000})) {
            character.setFormSkillTpCosts("androidforms", new Integer[]{42_000, 65_000, 104_000});
        }

        try {
            LOADER.saveConfig(RACES_DIR.resolve(race).resolve("character.json"), character);
        } catch (IOException ignored) {
        }
    }

    private static String dmzrevamp$normalizeRace(String raceName) {
        return raceName == null ? "human" : raceName.toLowerCase(Locale.ROOT)
                .replace("-", "").replace("_", "").replace(" ", "");
    }
}
