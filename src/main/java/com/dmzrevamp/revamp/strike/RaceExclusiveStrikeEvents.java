package com.dmzrevamp.revamp.strike;

import com.dmzrevamp.DmzRevampMod;
import com.dmzrevamp.racial.CustomRacialActionHelper;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ProgressionSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.techniques.EvasionAttackData;
import com.dragonminez.common.stats.techniques.PredefinedTechniques;
import com.dragonminez.common.stats.techniques.StrikeAttackData;
import com.dragonminez.common.stats.techniques.TechniqueData;
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
    public static final String SLEEP_RECOVERY_ID = "sleep_recovery";
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
            changed |= migrateSleepRecovery(data, eligibility.sleepRecovery);
            if (eligibility.namekianRegeneration) {
                changed |= grant(data, StrikeAttackTemplates.NAMEKIAN_REGENERATION);
            }
            if (changed) {
                NetworkHandler.sendToTrackingEntityAndSelf(new ProgressionSyncS2C(player), player);
            }
        });
    }

    /**
     * DMZ 2.2 moved Sleep Recovery into the native Evasion system. Replace any
     * pre-2.2 Overhaul Strike save with the native Evasion template while
     * preserving the equipped slot that already references the same id.
     */
    private static boolean migrateSleepRecovery(StatsData data, boolean eligible) {
        TechniqueData current = data.getTechniques().getUnlockedTechniques().get(SLEEP_RECOVERY_ID);
        if (!eligible) {
            if (current instanceof StrikeAttackData) {
                data.getTechniques().removeTechnique(SLEEP_RECOVERY_ID);
                return true;
            }
            return false;
        }
        if (current instanceof EvasionAttackData) {
            return false;
        }
        TechniqueData nativeSleep = PredefinedTechniques.copyOf(SLEEP_RECOVERY_ID);
        if (!(nativeSleep instanceof EvasionAttackData)) {
            return false;
        }
        // unlockTechnique replaces an existing object with the same id and does
        // not clear equipped slot strings, so legacy Majin loadouts are migrated.
        data.getTechniques().unlockTechnique(nativeSleep);
        return true;
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
                    && (!sleepRecovery || unlocked.get(SLEEP_RECOVERY_ID) instanceof EvasionAttackData)
                    && (!namekianRegeneration || unlocked.containsKey(StrikeAttackTemplates.NAMEKIAN_REGENERATION));
        }
    }
}
