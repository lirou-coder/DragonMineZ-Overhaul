package com.dmzrevamp.revamp.battlepower;

import com.dmzrevamp.config.KiSenseBlacklistConfig;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class BattlePowerReloadService {
    private BattlePowerReloadService() {}

    public static void recalculateAll(MinecraftServer server) {
        if (server == null) return;
        server.getPlayerList().getPlayers().forEach(player ->
                StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
                    if (data instanceof BattlePowerCacheControl cache) cache.dmzrevamp$recalculateBattlePower();
                    else data.getBattlePowerExact();
                }));

        for (var level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof LivingEntity living && !(living instanceof net.minecraft.world.entity.player.Player)) {
                    if (KiSenseBlacklistConfig.contains(living)) {
                        ManualBattlePowerStatEvents.clearBattlePower(living);
                    } else {
                        ManualBattlePowerStatEvents.syncDmzBattlePower(living);
                    }
                }
            }
        }
    }
}
