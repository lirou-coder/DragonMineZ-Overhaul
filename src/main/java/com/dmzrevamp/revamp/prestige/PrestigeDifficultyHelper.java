package com.dmzrevamp.revamp.prestige;

import com.dmzrevamp.config.LevelingRevampConfig;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

public final class PrestigeDifficultyHelper {
    private static final String QUEST_OWNER_TAG = "dmz_quest_owner";

    private PrestigeDifficultyHelper() {
    }

    public static double statMultiplier(Entity questEntity) {
        StatsData owner = ownerStats(questEntity);
        return owner == null ? 1D : PrestigeSystem.storyDifficultyMultiplier(owner);
    }

    public static StatsData ownerStats(Entity questEntity) {
        if (questEntity == null || !LevelingRevampConfig.prestigeEnabled()) return null;
        String ownerId = questEntity.getPersistentData().getString(QUEST_OWNER_TAG);
        if (ownerId.isBlank() || questEntity.getServer() == null) return null;
        try {
            ServerPlayer owner = questEntity.getServer().getPlayerList().getPlayer(UUID.fromString(ownerId));
            return owner == null ? null
                    : StatsProvider.get(StatsCapability.INSTANCE, owner).resolve().orElse(null);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
