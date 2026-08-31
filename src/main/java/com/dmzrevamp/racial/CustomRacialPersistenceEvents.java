package com.dmzrevamp.racial;

import com.dmzrevamp.DmzRevampMod;
import com.dmzrevamp.racial.impl.MajinRevampRacialSkill;
import com.dmzrevamp.racial.impl.SaiyanRpgZenkaiEvents;
import com.dmzrevamp.racial.impl.NamekianRevampRacialSkill;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CustomRacialPersistenceEvents {
    private static final String[] PERSISTENT_USE_TAGS = {
            SaiyanRpgZenkaiEvents.ZENKAI_USES_TAG,
            MajinRevampRacialSkill.ABSORPTION_USES_TAG,
            NamekianRevampRacialSkill.USES_TAG
    };
    private static final String[] PERSISTENT_LONG_TAGS = {
            SaiyanRpgZenkaiEvents.LAST_ZENKAI_USE_TAG,
            NamekianRevampRacialSkill.LAST_USE_TAG
    };

    private CustomRacialPersistenceEvents() {
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath() || !(event.getEntity() instanceof ServerPlayer newPlayer) || !(event.getOriginal() instanceof ServerPlayer oldPlayer)) {
            return;
        }

        copyPersistentUseCounters(oldPlayer, newPlayer);
        oldPlayer.reviveCaps();
        try {
            StatsData oldData = StatsProvider.get(StatsCapability.INSTANCE, oldPlayer).resolve().orElse(null);
            StatsData newData = StatsProvider.get(StatsCapability.INSTANCE, newPlayer).resolve().orElse(null);
            if (oldData != null && newData != null) {
                copyRacialCooldowns(oldData, newData);
            }
        } finally {
            oldPlayer.invalidateCaps();
        }
    }

    private static void copyPersistentUseCounters(ServerPlayer oldPlayer, ServerPlayer newPlayer) {
        for (String tag : PERSISTENT_USE_TAGS) {
            if (oldPlayer.getPersistentData().contains(tag)) {
                newPlayer.getPersistentData().putInt(tag, oldPlayer.getPersistentData().getInt(tag));
            }
        }
        for (String tag : PERSISTENT_LONG_TAGS) {
            if (oldPlayer.getPersistentData().contains(tag)) {
                newPlayer.getPersistentData().putLong(tag, oldPlayer.getPersistentData().getLong(tag));
            }
        }
    }

    private static void copyRacialCooldowns(StatsData oldData, StatsData newData) {
        for (CustomRacialSkill skill : CustomRacialSkillRegistry.values()) {
            String cooldownKey = skill.cooldownKey();
            if (cooldownKey == null || cooldownKey.isBlank()) {
                continue;
            }
            int cooldownTicks = oldData.getCooldowns().getCooldown(cooldownKey);
            if (cooldownTicks > 0) {
                newData.getCooldowns().setCooldown(cooldownKey, cooldownTicks);
            }
        }
    }
}
