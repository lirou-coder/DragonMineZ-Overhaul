package com.dmzrevamp.mixin;

import com.dragonminez.common.config.ConfigManager;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Mixin(ConfigManager.class)
public abstract class ConfigManagerEntitiesConfigOverrideMixin {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String ENTITIES_SNAPSHOT = "data/dmzrevamp/defaults/config/entities.json";
    private static final String RADIAL_LAYOUT_SNAPSHOT = "data/dmzrevamp/defaults/config/radial_layout.json";
    private static final String RACE_SNAPSHOT_ROOT = "data/dmzrevamp/defaults/races/";
    private static final String SAIRENS_DMZ_WORLD = "sairens_dmz_world";
    private static final String[] DEFAULT_RACES = {
            "bioandroid",
            "frostdemon",
            "human",
            "majin",
            "namekian",
            "saiyan"
    };

    @Shadow(remap = false)
    @Final
    private static Path CONFIG_DIR;

    @Inject(method = "initialize",
            at = @At(value = "INVOKE",
                    target = "Lcom/dragonminez/common/config/ConfigManager;loadGeneralConfigs()V",
                    shift = At.Shift.BEFORE),
            remap = false)
    private static void dmzrevamp$seedMissingEntitiesConfigOnInitialize(CallbackInfo ci) {
        dmzrevamp$seedMissingEntitiesConfig();
    }

    @Inject(method = "reload",
            at = @At(value = "INVOKE",
                    target = "Lcom/dragonminez/common/config/ConfigManager;loadGeneralConfigs()V",
                    shift = At.Shift.BEFORE),
            remap = false)
    private static void dmzrevamp$seedMissingEntitiesConfigOnReload(CallbackInfo ci) {
        dmzrevamp$seedMissingEntitiesConfig();
    }

    @Inject(method = "loadGeneralConfigs", at = @At("HEAD"), remap = false)
    private static void dmzrevamp$seedMissingEntitiesConfigBeforeGeneralLoad(CallbackInfo ci) {
        dmzrevamp$seedMissingEntitiesConfig();
    }

    private static void dmzrevamp$seedMissingEntitiesConfig() {
        dmzrevamp$copyMissingSnapshot(ENTITIES_SNAPSHOT, CONFIG_DIR.resolve("entities.json"), "Dragon Mine Z entities config");
        if (ModList.get().isLoaded(SAIRENS_DMZ_WORLD)) {
            return;
        }
        dmzrevamp$copyMissingSnapshot(RADIAL_LAYOUT_SNAPSHOT, CONFIG_DIR.resolve("radial_layout.json"), "Dragon Mine Z radial layout config");
        for (String race : DEFAULT_RACES) {
            dmzrevamp$copyMissingSnapshot(
                    RACE_SNAPSHOT_ROOT + race + "/character.json",
                    CONFIG_DIR.resolve("races").resolve(race).resolve("character.json"),
                    "Dragon Mine Z " + race + " character config"
            );
        }
    }

    private static void dmzrevamp$copyMissingSnapshot(String snapshot, Path target, String description) {
        if (Files.exists(target)) {
            return;
        }
        try {
            Files.createDirectories(target.getParent());
            try (InputStream stream = ConfigManagerEntitiesConfigOverrideMixin.class.getClassLoader().getResourceAsStream(snapshot)) {
                if (stream == null) {
                    LOGGER.warn("Could not create {} because {} was not found.", description, snapshot);
                    return;
                }
                Files.copy(stream, target, StandardCopyOption.REPLACE_EXISTING);
                LOGGER.info("Created missing {} from Dragon Mine Z: Overhaul defaults.", description);
            }
        } catch (Exception exception) {
            LOGGER.warn("Could not create missing {}: {}", description, exception.getMessage());
        }
    }
}
