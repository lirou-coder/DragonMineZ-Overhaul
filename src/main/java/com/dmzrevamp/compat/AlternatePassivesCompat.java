package com.dmzrevamp.compat;

import com.dragonminez.common.stats.StatsData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Optional bridge for Alternate Passives behavior that hooks DMZ's native racial implementation. */
public final class AlternatePassivesCompat {
    private static final String MOD_ID = "majinabsorption";
    private static final String ABSORPTION_SERVICE = "com.example.alternatepassives.core.AbsorptionService";
    private static volatile Method handleAbsorption;
    private static volatile boolean lookupAttempted;

    private AlternatePassivesCompat() {
    }

    /**
     * Passes a Majin Revamp absorption through Alternate Passives first.
     *
     * @return true when the addon consumed the racial action; false means the
     * Overhaul implementation should continue normally.
     */
    public static boolean handleMajinAbsorption(ServerPlayer player, StatsData data, LivingEntity target) {
        Method handler = absorptionHandler();
        if (handler == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(handler.invoke(null, player, data, target));
        } catch (IllegalAccessException | InvocationTargetException | LinkageError ignored) {
            // Keep the Overhaul racial usable if an incompatible addon version is installed.
            return false;
        }
    }

    private static Method absorptionHandler() {
        if (!ModList.get().isLoaded(MOD_ID)) {
            return null;
        }
        if (!lookupAttempted) {
            synchronized (AlternatePassivesCompat.class) {
                if (!lookupAttempted) {
                    lookupAttempted = true;
                    try {
                        Class<?> service = Class.forName(ABSORPTION_SERVICE);
                        handleAbsorption = service.getMethod("handleAbsorption",
                                ServerPlayer.class, StatsData.class, LivingEntity.class);
                    } catch (ReflectiveOperationException | LinkageError ignored) {
                        handleAbsorption = null;
                    }
                }
            }
        }
        return handleAbsorption;
    }
}
