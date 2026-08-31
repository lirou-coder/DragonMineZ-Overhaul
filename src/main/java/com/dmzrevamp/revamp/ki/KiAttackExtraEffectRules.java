package com.dmzrevamp.revamp.ki;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

public final class KiAttackExtraEffectRules {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path ROOT_DIR = FMLPaths.CONFIGDIR.get().resolve("dmzrevamp");
    private static final Path BENEFICIAL_BLACKLIST = ROOT_DIR.resolve("ki_attack_beneficial_effect_blacklist.json");
    private static final Path HARMFUL_BLACKLIST = ROOT_DIR.resolve("ki_attack_harmful_effect_blacklist.json");
    private static final Set<String> BLOCKED_PATHS = Set.of(
            "bad_omen",
            "hero_of_the_village",
            "raid_omen",
            "trial_omen",
            "confusion",
            "nausea",
            "majin",
            "mutant",
            "dash_cd",
            "doubledash_cd",
            "fused",
            "saiyan_passive",
            "bioandroid_passive",
            "frostdemon_passive",
            "human_passive",
            "namekian_passive",
            "majin_passive",
            "majin_revive",
            "ki_blast_cd",
            "poise_cd",
            "kicharge",
            "transform",
            "transformed",
            "stack_transform",
            "stack_transformed",
            "fly",
            "mightfruit",
            "candy",
            "ki_regen",
            "stamina_regen",
            "tp_gain",
            "mastery_gain",
            "absorption"
    );

    private static final Set<String> ALWAYS_DEFAULT_BLOCKED_IDS = Set.of(
            "dragonminez:stun",
            "dragonminez:stunned"
    );

    private static final String[] BLOCKED_KEYWORDS = {
            "cooldown",
            "_cd",
            "passive",
            "transform",
            "charging",
            "charge",
            "flying",
            "flight",
            "fly",
            "fusion",
            "fused",
            "racial",
            "skill",
            "zenkai",
            "absorption",
            "mastery",
            "tp_gain",
            "ki_regen",
            "stamina_regen"
    };

    private static Set<String> beneficialBlacklist;
    private static Set<String> harmfulBlacklist;

    private KiAttackExtraEffectRules() {
    }

    public static void reload() {
        beneficialBlacklist = null;
        harmfulBlacklist = null;
    }

    public static boolean isAllowed(ResourceLocation id) {
        if (id == null) {
            return false;
        }
        loadBlacklists();
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(id);
        if (effect == null) {
            return false;
        }
        return effect.isBeneficial()
                ? !beneficialBlacklist.contains(id.toString())
                : !harmfulBlacklist.contains(id.toString());
    }

    public static int minDurationSeconds(KiAttackExtraEffect.Mode mode) {
        return mode == KiAttackExtraEffect.Mode.HARMFUL ? 2 : 5;
    }

    public static int maxDurationSeconds(KiAttackExtraEffect.Mode mode) {
        return mode == KiAttackExtraEffect.Mode.HARMFUL ? 4 : 15;
    }

    public static int durationStepSeconds(KiAttackExtraEffect.Mode mode) {
        return mode == KiAttackExtraEffect.Mode.BENEFICIAL ? 5 : 1;
    }

    public static int clampDurationSeconds(KiAttackExtraEffect.Mode mode, int durationSeconds) {
        if (mode == KiAttackExtraEffect.Mode.NONE) {
            return 0;
        }
        int min = minDurationSeconds(mode);
        int max = maxDurationSeconds(mode);
        return Math.max(min, Math.min(max, durationSeconds));
    }

    public static int clampAppliedDurationTicks(KiAttackExtraEffect.Mode mode, int durationTicks, boolean targetIsPlayer) {
        if (mode == KiAttackExtraEffect.Mode.HARMFUL && targetIsPlayer) {
            return Math.min(durationTicks, maxDurationSeconds(mode) * 20);
        }
        return durationTicks;
    }

    private static void loadBlacklists() {
        if (beneficialBlacklist != null && harmfulBlacklist != null) {
            return;
        }
        try {
            Files.createDirectories(ROOT_DIR);
            if (Files.notExists(BENEFICIAL_BLACKLIST) || Files.notExists(HARMFUL_BLACKLIST)) {
                writeMissingDefaultBlacklists();
            }
            beneficialBlacklist = readBlacklist(BENEFICIAL_BLACKLIST);
            harmfulBlacklist = readBlacklist(HARMFUL_BLACKLIST);
        } catch (Exception ignored) {
            beneficialBlacklist = new LinkedHashSet<>();
            harmfulBlacklist = defaultBlacklist(false);
        }
    }

    private static void writeMissingDefaultBlacklists() throws IOException {
        Set<String> beneficialDefaults = defaultBlacklist(true);
        Set<String> harmfulDefaults = defaultBlacklist(false);
        if (Files.notExists(BENEFICIAL_BLACKLIST)) {
            writeBlacklist(BENEFICIAL_BLACKLIST, beneficialDefaults);
        }
        if (Files.notExists(HARMFUL_BLACKLIST)) {
            writeBlacklist(HARMFUL_BLACKLIST, harmfulDefaults);
        }
    }

    private static Set<String> defaultBlacklist(boolean beneficial) {
        Set<String> ids = new LinkedHashSet<>();
        ForgeRegistries.MOB_EFFECTS.getEntries().stream()
                .filter(entry -> entry.getValue().isBeneficial() == beneficial)
                .map(entry -> entry.getKey().location())
                .filter(KiAttackExtraEffectRules::matchesDefaultBlacklist)
                .map(ResourceLocation::toString)
                .sorted()
                .forEach(ids::add);
        return ids;
    }

    private static boolean matchesDefaultBlacklist(ResourceLocation id) {
        String namespace = id.getNamespace();
        String path = id.getPath();
        if ("dmzrevamp".equals(namespace) || ALWAYS_DEFAULT_BLOCKED_IDS.contains(id.toString()) || BLOCKED_PATHS.contains(path)) {
            return true;
        }
        for (String keyword : BLOCKED_KEYWORDS) {
            if (path.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> readBlacklist(Path path) throws IOException {
        Set<String> ids = new LinkedHashSet<>();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (!element.isJsonArray()) {
                return ids;
            }
            for (JsonElement entry : element.getAsJsonArray()) {
                if (entry.isJsonPrimitive()) {
                    String id = entry.getAsString();
                    if (ResourceLocation.tryParse(id) != null) {
                        ids.add(id);
                    }
                }
            }
        }
        return ids;
    }

    private static void writeBlacklist(Path path, Set<String> ids) throws IOException {
        JsonArray array = new JsonArray();
        ids.forEach(array::add);
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(array, writer);
        }
    }
}
