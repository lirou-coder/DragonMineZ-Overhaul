package com.dmzrevamp.compat;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;

public final class DmzKiOverchargeCompat {
    public static final String MODID = "dmzkiovercharge";

    private DmzKiOverchargeCompat() {
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

    public static float effectiveMaxChargePercent(float fallback) {
        if (!isLoaded() && !isLoadedEarly()) {
            return fallback;
        }
        try {
            Object value = Class.forName("com.lcd.dmzkiovercharge.config.OverchargeConfig")
                    .getMethod("effectiveMaxChargePercent")
                    .invoke(null);
            if (value instanceof Number number) {
                float percent = number.floatValue();
                if (Float.isFinite(percent) && percent > 0.0F) {
                    return percent;
                }
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
        }
        return fallback;
    }
}
