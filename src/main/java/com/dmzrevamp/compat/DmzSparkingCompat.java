package com.dmzrevamp.compat;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;

public final class DmzSparkingCompat {
    public static final String MODID = "dmzsparking";

    private DmzSparkingCompat() {
    }

    public static boolean isLoaded() {
        try {
            return ModList.get().isLoaded(MODID);
        } catch (Throwable ignored) {
            return isLoadedEarly();
        }
    }

    public static boolean isLoadedEarly() {
        try {
            return FMLLoader.getLoadingModList() != null
                    && FMLLoader.getLoadingModList().getModFileById(MODID) != null;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
