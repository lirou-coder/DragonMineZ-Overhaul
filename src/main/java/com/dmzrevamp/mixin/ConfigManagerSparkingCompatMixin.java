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
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Loaded exclusively when dmzsparking is present; see DmzRevampMixinPlugin. */
@Mixin(value = ConfigManager.class, remap = false)
public abstract class ConfigManagerSparkingCompatMixin {
    private static final Set<String> DMZREVAMP_BASE_RACES = Set.of(
            "saiyan", "human", "namekian", "majin", "bioandroid", "frostdemon"
    );
    private static final ThreadLocal<ArrayDeque<Boolean>> DMZREVAMP_GENERATING_CHARACTER =
            ThreadLocal.withInitial(ArrayDeque::new);
    @Shadow @Final private static Map<String, RaceCharacterConfig> RACE_CHARACTER;
    @Shadow @Final private static Path RACES_DIR;
    @Shadow @Final private static ConfigLoader LOADER;

    @Inject(method = "createOrLoadRace", at = @At("HEAD"))
    private static void dmzrevamp$rememberCharacterGeneration(String raceName, boolean defaultRace, CallbackInfo ci) {
        String race = raceName == null ? "human" : raceName.toLowerCase(Locale.ROOT);
        Path characterPath = RACES_DIR.resolve(race).resolve("character.json");
        DMZREVAMP_GENERATING_CHARACTER.get().push(Files.notExists(characterPath));
    }

    @Inject(method = "createOrLoadRace", at = @At("TAIL"))
    private static void dmzrevamp$writeSparkingRacial(String raceName, boolean defaultRace, CallbackInfo ci) {
        ArrayDeque<Boolean> generationStack = DMZREVAMP_GENERATING_CHARACTER.get();
        boolean generating = !generationStack.isEmpty() && generationStack.pop();
        if (generationStack.isEmpty()) DMZREVAMP_GENERATING_CHARACTER.remove();
        // Existing files are authoritative. In particular, never overwrite a
        // racialSkill selected by a pack author or server administrator on reload.
        if (!generating) return;

        String race = raceName == null ? "human" : raceName.toLowerCase(Locale.ROOT);
        String normalizedRace = race.replace("-", "").replace("_", "").replace(" ", "");
        if (!DMZREVAMP_BASE_RACES.contains(normalizedRace)) return;
        RaceCharacterConfig character = RACE_CHARACTER.get(race);
        if (character == null) return;
        character.setRacialSkill(normalizedRace + "revamp");
        try {
            LOADER.saveConfig(RACES_DIR.resolve(race).resolve("character.json"), character);
        } catch (IOException ignored) {
        }
    }
}
