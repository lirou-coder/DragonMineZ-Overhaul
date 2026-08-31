package com.dmzrevamp.revamp.strike;

import com.dmzrevamp.DmzRevampMod;
import com.dmzrevamp.racial.CustomRacialActionHelper;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ProgressionSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.techniques.StrikeAttackData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RaceExclusiveStrikeEvents {
    private static final boolean SAIRENS_WORLD_LOADED = ModList.get().isLoaded("sairens_dmz_world");
    private static final Map<UUID, Eligibility> LAST_ELIGIBILITY = new ConcurrentHashMap<>();
    private RaceExclusiveStrikeEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            grantMissing(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide() || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % 40 == 0) {
            grantMissing(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_ELIGIBILITY.remove(event.getEntity().getUUID());
    }

    private static void grantMissing(ServerPlayer player) {
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            if (!data.getStatus().isHasCreatedCharacter()) {
                LAST_ELIGIBILITY.remove(player.getUUID());
                return;
            }
            Eligibility eligibility = eligibility(data);
            Eligibility previous = LAST_ELIGIBILITY.put(player.getUUID(), eligibility);
            if (eligibility.equals(previous) && eligibility.hasAllRequired(data)) {
                return;
            }
            boolean changed = false;
            if (eligibility.androidAbsorption) {
                changed |= grant(data, StrikeAttackTemplates.ANDROID_ABSORPTION);
            }
            if (eligibility.sleepRecovery) {
                changed |= grant(data, StrikeAttackTemplates.SLEEP_RECOVERY);
            }
            if (eligibility.namekianRegeneration) {
                changed |= grant(data, StrikeAttackTemplates.NAMEKIAN_REGENERATION);
            }
            if (changed) {
                NetworkHandler.sendToTrackingEntityAndSelf(new ProgressionSyncS2C(player), player);
            }
        });
    }

    private static Eligibility eligibility(StatsData data) {
        String racial = CustomRacialActionHelper.getConfiguredRacialSkillId(data);
        String race = data.getCharacter().getRaceName();
        boolean bioAndroid = "bioandroidrevamp".equalsIgnoreCase(racial) || "bioandroid".equalsIgnoreCase(race);
        boolean human = "humanrevamp".equalsIgnoreCase(racial) || "human".equalsIgnoreCase(race);
        boolean majin = "majinrevamp".equalsIgnoreCase(racial) || "majin".equalsIgnoreCase(race);
        boolean namekian = "namekianrevamp".equalsIgnoreCase(racial) || "namekian".equalsIgnoreCase(race);
        return new Eligibility(bioAndroid || (human && data.getStatus().isAndroidUpgraded()),
                majin, namekian || (!SAIRENS_WORLD_LOADED && bioAndroid));
    }

    private static boolean grant(StatsData data, String id) {
        if (data.getTechniques().getUnlockedTechniques().containsKey(id)) {
            return false;
        }
        StrikeAttackData technique = StrikeAttackTemplates.copy(id);
        if (technique == null) {
            return false;
        }
        data.getTechniques().unlockTechnique(technique);
        return true;
    }

    private record Eligibility(boolean androidAbsorption, boolean sleepRecovery, boolean namekianRegeneration) {
        private boolean hasAllRequired(StatsData data) {
            var unlocked = data.getTechniques().getUnlockedTechniques();
            return (!androidAbsorption || unlocked.containsKey(StrikeAttackTemplates.ANDROID_ABSORPTION))
                    && (!sleepRecovery || unlocked.containsKey(StrikeAttackTemplates.SLEEP_RECOVERY))
                    && (!namekianRegeneration || unlocked.containsKey(StrikeAttackTemplates.NAMEKIAN_REGENERATION));
        }
    }
}
