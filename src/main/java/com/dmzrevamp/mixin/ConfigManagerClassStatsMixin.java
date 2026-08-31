package com.dmzrevamp.mixin;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.dmzrevamp.config.CustomBattlePowerConfig;
import com.dmzrevamp.config.CustomStrikeAttacksConfig;
import com.dmzrevamp.config.DynamicGrowthCurveConfig;
import com.dmzrevamp.config.FusionsRevampedConfig;
import com.dmzrevamp.config.KiClashConfigured;
import com.dmzrevamp.config.StrikeClashConfigured;
import com.dmzrevamp.config.LevelingRevampConfig;
import com.dmzrevamp.config.WeightMovementPenaltyConfig;
import com.dmzrevamp.config.AdaptiveDefenseMoreConfigured;
import com.dmzrevamp.config.racial.DmzRevampRacialConfigs;
import com.dmzrevamp.revamp.classes.DmzClassConfigManager;
import com.dmzrevamp.compat.AttributeFixCompatEvents;
import com.dmzrevamp.revamp.ki.KiAttackExtraEffectRules;
import com.dmzrevamp.revamp.entities.TransformChainConfig;
import com.dragonminez.common.config.ConfigLoader;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.config.RaceStatsConfig;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.IConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Mixin(ConfigManager.class)
public abstract class ConfigManagerClassStatsMixin {
    private static final String[] DMZREVAMP_DEFAULT_RACE_ORDER = {
            "human",
            "saiyan",
            "namekian",
            "frostdemon",
            "bioandroid",
            "majin"
    };

    @Shadow(remap = false)
    @Final
    private static Map<String, RaceStatsConfig> RACE_STATS;

    @Shadow(remap = false)
    @Final
    private static Path RACES_DIR;

    @Shadow(remap = false)
    @Final
    private static ConfigLoader LOADER;

    @Inject(method = "initialize", at = @At("HEAD"), remap = false)
    private static void dmzrevamp$reloadSeparatedClassConfigs(CallbackInfo ci) {
        dmzrevamp$reloadRevampConfigs();
    }

    @Inject(method = "reload", at = @At("HEAD"), remap = false)
    private static void dmzrevamp$reloadSeparatedClassConfigsOnDmzReload(CallbackInfo ci) {
        dmzrevamp$reloadRevampConfigs();
    }

    @Inject(method = {"initialize", "reload"}, at = @At("RETURN"), remap = false)
    private static void dmzrevamp$refreshAttributeCapsAfterDmzConfigs(CallbackInfo ci) {
        // Reading maxValue while ConfigManager is between clearing and loading its
        // files briefly installs the fallback cap and makes derived resources flicker.
        AttributeFixCompatEvents.refreshMainAttributeCaps();
    }

    private static void dmzrevamp$reloadRevampConfigs() {
        dmzrevamp$reloadForgeCommonConfig();
        AdaptiveDefenseMoreConfigured.reload();
        DmzRevampRacialConfigs.loadAll();
        KiAttackExtraEffectRules.reload();
        DmzClassConfigManager.reload();
        CustomBattlePowerConfig.reload();
        CustomStrikeAttacksConfig.reload();
        DynamicGrowthCurveConfig.reload();
        FusionsRevampedConfig.reload();
        KiClashConfigured.reload();
        StrikeClashConfigured.reload();
        LevelingRevampConfig.reload();
        TransformChainConfig.reload();
        WeightMovementPenaltyConfig.reload();
    }

    private static void dmzrevamp$reloadForgeCommonConfig() {
        ModConfig commonConfig = ConfigTracker.INSTANCE.fileMap().get("dmzrevamp-common.toml");
        if (commonConfig == null || !(commonConfig.getConfigData() instanceof CommentedFileConfig fileConfig)) {
            return;
        }

        try {
            fileConfig.load();
            IConfigSpec<?> spec = commonConfig.getSpec();
            spec.acceptConfig(fileConfig);
            spec.afterReload();
        } catch (RuntimeException ignored) {
            // Retain the last valid Forge values if the TOML is temporarily
            // unreadable or invalid; the other revamp configs can still reload.
        }
    }

    @Inject(method = "createOrLoadRace", at = @At("TAIL"), remap = false)
    private static void dmzrevamp$prepareLoadedRaceStats(String raceName, boolean defaultRace, CallbackInfo ci) {
        String normalizedRace = raceName != null ? raceName.toLowerCase(Locale.ROOT) : "human";
        RaceStatsConfig raceStats = RACE_STATS.get(normalizedRace);
        if (raceStats == null) {
            return;
        }

        RaceStatsConfig separatedStats = DmzClassConfigManager.createSeparatedRaceStats(normalizedRace, raceStats);
        Path statsPath = RACES_DIR.resolve(normalizedRace).resolve("stats.json");
        try {
            if (DmzClassConfigManager.isVanillaClassStatsConfig(raceStats)) {
                Files.deleteIfExists(statsPath);
            }
            LOADER.saveConfig(statsPath, separatedStats);
        } catch (IOException ignored) {
        }
        RACE_STATS.put(normalizedRace, separatedStats);
        DmzClassConfigManager.prepareRaceStats(normalizedRace, separatedStats);
    }

    @Inject(method = "getRaceStats", at = @At("RETURN"), remap = false)
    // Handles the prepareRaceStats logic for this class.
    private static void dmzrevamp$prepareRaceStats(String raceName, CallbackInfoReturnable<RaceStatsConfig> cir) {
        DmzClassConfigManager.prepareRaceStats(raceName, cir.getReturnValue());
    }

    @Inject(method = "getAllRaceStats", at = @At("RETURN"), cancellable = true, remap = false)
    // Handles the prepareAllRaceStats logic for this class.
    private static void dmzrevamp$prepareAllRaceStats(CallbackInfoReturnable<Map<String, RaceStatsConfig>> cir) {
        DmzClassConfigManager.prepareAllRaceStats(cir.getReturnValue());
        cir.setReturnValue(dmzrevamp$orderedRaceMap(cir.getReturnValue()));
    }

    @Inject(method = "getAllRaceCharacters", at = @At("RETURN"), cancellable = true, remap = false)
    private static void dmzrevamp$orderAllRaceCharacters(CallbackInfoReturnable<Map<String, RaceCharacterConfig>> cir) {
        cir.setReturnValue(dmzrevamp$orderedRaceMap(cir.getReturnValue()));
    }

    @Inject(method = "getLoadedRaces", at = @At("RETURN"), cancellable = true, remap = false)
    private static void dmzrevamp$orderLoadedRaces(CallbackInfoReturnable<List<String>> cir) {
        cir.setReturnValue(dmzrevamp$orderedRaceList(cir.getReturnValue()));
    }

    @Inject(method = "getDefaultRaces", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dmzrevamp$canonicalHudRaceAtlasOrder(CallbackInfoReturnable<List<String>> cir) {
        // Both vanilla HUDs convert the race to a racial_icons.png X coordinate using
        // this list's index. Never expose ConfigManager.DEFAULT_RACES through
        // Arrays.asList: sorting that list mutates the backing array and corrupts every icon.
        cir.setReturnValue(new ArrayList<>(Arrays.asList(DMZREVAMP_DEFAULT_RACE_ORDER)));
    }

    private static <T> Map<String, T> dmzrevamp$orderedRaceMap(Map<String, T> raceMap) {
        LinkedHashMap<String, T> ordered = new LinkedHashMap<>();
        if (raceMap == null || raceMap.isEmpty()) {
            return ordered;
        }

        // Keeps the vanilla DMZ race order when code reads race config maps.
        for (String race : DMZREVAMP_DEFAULT_RACE_ORDER) {
            T value = dmzrevamp$getRaceMapValue(raceMap, race);
            if (value != null) {
                ordered.put(race, value);
            }
        }
        raceMap.entrySet().stream()
                .filter(entry -> !dmzrevamp$containsRaceKey(ordered, entry.getKey()))
                .sorted(Comparator.comparing(entry -> entry.getKey().toLowerCase(Locale.ROOT)))
                .forEach(entry -> ordered.put(entry.getKey(), entry.getValue()));
        return ordered;
    }

    private static List<String> dmzrevamp$orderedRaceList(List<String> races) {
        ArrayList<String> ordered = new ArrayList<>();
        if (races == null || races.isEmpty()) {
            return ordered;
        }

        // RaceSelectionScreen reads this list directly, so this is the visible race button order.
        for (String race : DMZREVAMP_DEFAULT_RACE_ORDER) {
            for (String loadedRace : races) {
                if (race.equalsIgnoreCase(loadedRace) && !dmzrevamp$containsRace(ordered, loadedRace)) {
                    ordered.add(loadedRace);
                }
            }
        }
        races.stream()
                .filter(race -> !dmzrevamp$containsRace(ordered, race))
                .sorted(Comparator.comparing(race -> race == null ? "" : race.toLowerCase(Locale.ROOT)))
                .forEach(ordered::add);
        return ordered;
    }

    private static <T> T dmzrevamp$getRaceMapValue(Map<String, T> raceMap, String raceId) {
        T exactValue = raceMap.get(raceId);
        if (exactValue != null) {
            return exactValue;
        }

        for (Map.Entry<String, T> entry : raceMap.entrySet()) {
            if (raceId.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static boolean dmzrevamp$containsRaceKey(Map<String, ?> raceMap, String raceId) {
        for (String key : raceMap.keySet()) {
            if (key.equalsIgnoreCase(raceId)) {
                return true;
            }
        }
        return false;
    }

    private static boolean dmzrevamp$containsRace(List<String> races, String raceId) {
        for (String race : races) {
            if (race.equalsIgnoreCase(raceId)) {
                return true;
            }
        }
        return false;
    }
}
