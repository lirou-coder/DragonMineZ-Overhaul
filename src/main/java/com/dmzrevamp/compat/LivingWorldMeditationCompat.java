package com.dmzrevamp.compat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

/** Optional bridge to Living World's public meditation integration surface. */
public final class LivingWorldMeditationCompat {
    private static final String MOD_ID = "dmzlivingworld";
    private static boolean resolved;
    private static Method isMeditating;

    private LivingWorldMeditationCompat() {
    }

    public static boolean isMeditating(ServerPlayer player) {
        if (player == null || !player.isPassenger() || !ModList.get().isLoaded(MOD_ID)) {
            return false;
        }
        resolve();
        if (isMeditating == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(isMeditating.invoke(null, player));
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    private static synchronized void resolve() {
        if (resolved) return;
        resolved = true;
        try {
            Class<?> api = Class.forName("com.kunyo.dbzmeditation.MeditationIntegrationApi");
            isMeditating = api.getMethod("isMeditating", ServerPlayer.class);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            isMeditating = null;
        }
    }
}
