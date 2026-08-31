package com.dmzrevamp.racial;

import com.dragonminez.common.stats.StatsData;
import net.minecraft.server.level.ServerPlayer;

/** Uses an epoch timestamp in addition to DMZ's ticking cooldown so relogs and restarts cannot bypass it. */
public final class PersistentRacialCooldown {
    private PersistentRacialCooldown() {}

    public static boolean isActive(ServerPlayer player, StatsData data, String cooldownKey,
                                   String lastUseTag, int cooldownSeconds) {
        int configuredTicks = safeTicks(cooldownSeconds);
        if (configuredTicks <= 0) return false;

        long lastUse = player.getPersistentData().getLong(lastUseTag);
        long durationMillis = configuredTicks * 50L;
        long elapsed = System.currentTimeMillis() - lastUse;
        long remainingMillis = lastUse <= 0L ? 0L : Math.max(0L, durationMillis - Math.max(0L, elapsed));
        int persistentTicks = (int) Math.min(Integer.MAX_VALUE, (remainingMillis + 49L) / 50L);
        int dmzTicks = data.getCooldowns().getCooldown(cooldownKey);

        if (persistentTicks > dmzTicks) data.getCooldowns().setCooldown(cooldownKey, persistentTicks);
        return persistentTicks > 0 || dmzTicks > 0;
    }

    public static void markUsed(ServerPlayer player, StatsData data, String cooldownKey,
                                String lastUseTag, int cooldownSeconds) {
        int ticks = safeTicks(cooldownSeconds);
        player.getPersistentData().putLong(lastUseTag, System.currentTimeMillis());
        if (ticks > 0) data.getCooldowns().setCooldown(cooldownKey, ticks);
    }

    public static void clear(ServerPlayer player, StatsData data, String cooldownKey, String lastUseTag) {
        player.getPersistentData().remove(lastUseTag);
        data.getCooldowns().removeCooldown(cooldownKey);
    }

    private static int safeTicks(int seconds) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, (long) seconds * 20L));
    }
}
