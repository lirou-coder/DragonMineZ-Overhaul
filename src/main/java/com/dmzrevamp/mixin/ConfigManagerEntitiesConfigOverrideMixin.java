package com.dmzrevamp.mixin;

import com.dragonminez.common.config.ConfigManager;
import com.dmzrevamp.compat.NoeaConfigCompat;
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

@Mixin(value = ConfigManager.class, priority = 2000)
public abstract class ConfigManagerEntitiesConfigOverrideMixin {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String ENTITIES_SNAPSHOT = "data/dmzrevamp/defaults/config/entities.json";
    private static final String RADIAL_LAYOUT_SNAPSHOT = "data/dmzrevamp/defaults/config/radial_layout.json";
    private static final String RAID_DEFAULTS_ROOT = "data/dmzrevamp/defaults/config/raids/";
    private static final String RAID_DEFAULTS_INDEX = RAID_DEFAULTS_ROOT + "index.txt";
    private static final String TOURNAMENT_DEFAULTS_ROOT = "data/dmzrevamp/defaults/config/tournaments/";
    private static final String TOURNAMENT_DEFAULTS_INDEX = TOURNAMENT_DEFAULTS_ROOT + "index.txt";
    private static final String SAIRENS_DMZ_WORLD = "sairens_dmz_world";

    @Shadow(remap = false)
    @Final
    private static Path CONFIG_DIR;

    @Inject(method = "initialize", at = @At("HEAD"), remap = false)
    private static void dmzrevamp$seedBaseBeforeAddonInstallers(CallbackInfo ci) {
        // Priority 2000: this runs before Noea's default-priority HEAD installer.
        NoeaConfigCompat.prepareBaseDefaults();
    }

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
        dmzrevamp$copyMissingIndexedSnapshots(RAID_DEFAULTS_INDEX, RAID_DEFAULTS_ROOT,
                CONFIG_DIR.resolve("raids"), "raid");
        dmzrevamp$copyMissingIndexedSnapshots(TOURNAMENT_DEFAULTS_INDEX, TOURNAMENT_DEFAULTS_ROOT,
                CONFIG_DIR.resolve("tournaments"), "tournament");
        if (ModList.get().isLoaded(SAIRENS_DMZ_WORLD)) {
            return;
        }
        dmzrevamp$copyMissingSnapshot(RADIAL_LAYOUT_SNAPSHOT, CONFIG_DIR.resolve("radial_layout.json"), "Dragon Mine Z radial layout config");
    }

    private static void dmzrevamp$copyMissingIndexedSnapshots(String indexPath, String snapshotRoot,
                                                               Path targetRoot, String description) {
        try (InputStream index = ConfigManagerEntitiesConfigOverrideMixin.class.getClassLoader()
                .getResourceAsStream(indexPath)) {
            if (index == null) return;
            String entries = new String(index.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            for (String entry : entries.lines().map(String::trim)
                    .filter(line -> !line.isBlank() && line.endsWith(".json") && !line.contains(".."))
                    .toList()) {
                Path target = targetRoot.resolve(entry).normalize();
                if (!target.startsWith(targetRoot.normalize()) || Files.exists(target)) continue;
                String snapshot = snapshotRoot + entry.replace('\\', '/');
                dmzrevamp$copyMissingSnapshot(snapshot, target,
                        "Dragon Mine Z " + description + " config " + entry);
            }
        } catch (Exception exception) {
            LOGGER.warn("Could not seed missing Dragon Mine Z {} defaults: {}", description, exception.getMessage());
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
