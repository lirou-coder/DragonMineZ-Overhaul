package com.dmzrevamp.compat;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;

import java.lang.reflect.Method;

/** Optional integration with DMZ Ragnarok's SDU editor. */
public final class SduCompat {
    public static final String MODID = "dmz_ragnarok";
    private static final String LEGACY_MODID = "sdu";
    private static final String SUPPRESSION_CONFIG = "net.shurui.dev.sdu.race.SuppressedDefaultsConfig";

    private static Method isClassSuppressed;
    private static boolean suppressionLookupAttempted;

    private SduCompat() {
    }

    public static boolean isLoaded() {
        try {
            ModList modList = ModList.get();
            return modList != null && (modList.isLoaded(MODID) || modList.isLoaded(LEGACY_MODID));
        } catch (Throwable ignored) {
            return isLoadedEarly();
        }
    }

    public static boolean isLoadedEarly() {
        try {
            return FMLLoader.getLoadingModList().getModFileById(MODID) != null
                    || FMLLoader.getLoadingModList().getModFileById(LEGACY_MODID) != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Reads SDU's persisted suppression state without making SDU a required
     * compile-time or runtime dependency.
     */
    public static boolean isClassSuppressed(String classId) {
        if (!isLoaded() || classId == null || classId.isBlank()) {
            return false;
        }

        Method method = resolveSuppressionMethod();
        if (method == null) {
            return false;
        }

        try {
            return Boolean.TRUE.equals(method.invoke(null, classId));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    private static synchronized Method resolveSuppressionMethod() {
        if (suppressionLookupAttempted) {
            return isClassSuppressed;
        }
        suppressionLookupAttempted = true;

        try {
            Class<?> configClass = Class.forName(SUPPRESSION_CONFIG, false, SduCompat.class.getClassLoader());
            isClassSuppressed = configClass.getMethod("isClassSuppressed", String.class);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            isClassSuppressed = null;
        }
        return isClassSuppressed;
    }
}
