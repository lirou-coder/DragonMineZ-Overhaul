package com.dmzrevamp.revamp.fusion;

import com.dmzrevamp.DmzRevampMod;
import com.dmzrevamp.config.FusionsRevampedConfig;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FusionRevampEvents {
    private static final Map<UUID, PendingFusionResources> PENDING_RESOURCE_AVERAGES = new ConcurrentHashMap<>();
    private static final Map<UUID, FusionSyncState> SYNC_STATES = new ConcurrentHashMap<>();

    private FusionRevampEvents() {
    }

    public static void captureAndScheduleResourceAverage(StatsData leaderData, StatsData partnerData) {
        if (!(leaderData.getPlayer() instanceof ServerPlayer leader) || !(partnerData.getPlayer() instanceof ServerPlayer partner)) {
            return;
        }
        // DMZ finishes fusion state and attribute sync after applying bonuses, so resources are restored on the next player tick.
        double health = averagePercent(leader.getHealth(), leader.getMaxHealth(), partner.getHealth(), partner.getMaxHealth());
        double energy = averagePercent(leaderData.getResources().getCurrentEnergy(), leaderData.getMaxEnergy(),
                partnerData.getResources().getCurrentEnergy(), partnerData.getMaxEnergy());
        double stamina = averagePercent(leaderData.getResources().getCurrentStamina(), leaderData.getMaxStamina(),
                partnerData.getResources().getCurrentStamina(), partnerData.getMaxStamina());
        PENDING_RESOURCE_AVERAGES.put(leader.getUUID(),
                new PendingFusionResources(leader.getUUID(), partner.getUUID(), 10, health, energy, stamina));
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide() || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        processPendingResourceAverage(player);
        syncObserverMultipliers(player);
    }

    private static void processPendingResourceAverage(ServerPlayer player) {
        PendingFusionResources pending = PENDING_RESOURCE_AVERAGES.get(player.getUUID());
        if (pending == null) {
            return;
        }
        ServerPlayer partner = player.server.getPlayerList().getPlayer(pending.partnerId);
        StatsData leaderData = data(player);
        StatsData partnerData = data(partner);
        if (leaderData != null && partnerData != null && leaderData.getStatus().isFused()) {
            FusionRevampLogic.restoreFusionResources(leaderData, partnerData,
                    pending.healthPercent, pending.energyPercent, pending.staminaPercent, pending.ticksRemaining == 10);
        }
        if (pending.ticksRemaining > 1) PENDING_RESOURCE_AVERAGES.put(player.getUUID(), pending.nextTick());
        else PENDING_RESOURCE_AVERAGES.remove(player.getUUID());
    }

    private static void syncObserverMultipliers(ServerPlayer player) {
        StatsData data = data(player);
        if (data == null || !data.getStatus().isFused() || !data.getStatus().isFusionLeader()) {
            SYNC_STATES.remove(player.getUUID());
            return;
        }
        ServerPlayer partner = player.server.getPlayerList().getPlayer(data.getStatus().getFusionPartnerUUID());
        StatsData partnerData = data(partner);
        if (partnerData != null) {
            boolean multipliersChanged = FusionRevampLogic.mirrorLeaderMultipliersToObserver(data, partnerData);
            boolean resourcesChanged = FusionRevampLogic.synchronizeSharedFusionState(data, partnerData);
            long leaderBonuses = FusionRevampLogic.ownBonusSignature(data);
            long partnerBonuses = FusionRevampLogic.ownBonusSignature(partnerData);
            FusionSyncState previous = SYNC_STATES.get(player.getUUID());
            boolean sourcesChanged = previous == null
                    || previous.leaderBonuses != leaderBonuses
                    || previous.partnerBonuses != partnerBonuses;
            boolean bonusesChanged = sourcesChanged && FusionRevampLogic.mirrorBonusesForPair(data, partnerData);
            SYNC_STATES.put(player.getUUID(), new FusionSyncState(leaderBonuses, partnerBonuses));
            if (resourcesChanged || bonusesChanged || multipliersChanged) {
                FusionRevampLogic.syncFusionPair(data, partnerData);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        PENDING_RESOURCE_AVERAGES.remove(id);
        SYNC_STATES.remove(id);
    }

    @SubscribeEvent
    public static void onFormChange(DMZEvent.FormChangeEvent event) {
        lowerFusionTimer(event.getPlayer(),
                ConfigManager.getForm(raceName(event.getPlayer()), event.getOldGroup(), event.getOldForm()),
                ConfigManager.getForm(raceName(event.getPlayer()), event.getNewGroup(), event.getNewForm()),
                event.getNewForm());
    }

    @SubscribeEvent
    public static void onStackFormChange(DMZEvent.StackFormChangeEvent event) {
        lowerFusionTimer(event.getPlayer(),
                ConfigManager.getStackForm(event.getOldGroup(), event.getOldForm()),
                ConfigManager.getStackForm(event.getNewGroup(), event.getNewForm()),
                event.getNewForm());
    }

    private static void lowerFusionTimer(ServerPlayer player, FormConfig.FormData previous,
                                         FormConfig.FormData current, String currentFormName) {
        if (player == null || currentFormName == null || currentFormName.isBlank()
                || current == null || !FusionsRevampedConfig.isRevampedEnabled()
                || !FusionsRevampedConfig.get().fusionRevamped.transformationsLowerFusedTimer) {
            return;
        }
        StatsData leaderData = data(player);
        if (leaderData == null || !leaderData.getStatus().isFused()
                || !leaderData.getStatus().isFusionLeader()) {
            return;
        }

        double previousAverage = formMultiplierAverage(previous);
        double currentAverage = formMultiplierAverage(current);
        double increase = currentAverage - previousAverage;
        if (!Double.isFinite(increase) || increase <= 0D) return;

        int currentTimer = leaderData.getStatus().getFusionTimer();
        if (currentTimer <= 0) return;
        double divisor = 1D + (increase / 10D)
                * FusionsRevampedConfig.get().fusionRevamped.fusionTransformationDecreaseMultiplier;
        int reducedTimer = Math.max(0, (int) Math.floor(currentTimer / divisor));
        if (reducedTimer >= currentTimer) return;

        leaderData.getStatus().setFusionTimer(reducedTimer);
        synchronizeFusedEffect(player, reducedTimer);
        StatsData partnerData = FusionRevampLogic.getFusionPartnerData(leaderData);
        if (partnerData != null) {
            partnerData.getStatus().setFusionTimer(reducedTimer);
            if (partnerData.getPlayer() instanceof ServerPlayer partner) {
                synchronizeFusedEffect(partner, reducedTimer);
            }
            FusionRevampLogic.syncFusionPair(leaderData, partnerData);
        } else {
            FusionRevampLogic.syncFusionPair(leaderData, leaderData);
        }
    }

    private static void synchronizeFusedEffect(ServerPlayer player, int remainingTicks) {
        if (player == null) return;
        // Adding a shorter instance does not replace a longer active effect in
        // vanilla. Remove it first so the client immediately receives the new
        // authoritative remaining duration.
        player.removeEffect(MainEffects.FUSED.get());
        if (remainingTicks > 0) {
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    MainEffects.FUSED.get(), remainingTicks, 0, false, false));
        }
    }

    private static String raceName(ServerPlayer player) {
        StatsData data = data(player);
        return data == null ? "" : data.getCharacter().getRaceName();
    }

    private static double formMultiplierAverage(FormConfig.FormData form) {
        if (form == null) return 1D;
        return (form.getStrMultiplier() + form.getSkpMultiplier() + form.getStmMultiplier()
                + form.getDefMultiplier() + form.getVitMultiplier() + form.getPwrMultiplier()
                + form.getEneMultiplier()) / 7D;
    }

    private static StatsData data(ServerPlayer player) {
        return player == null ? null : StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
    }

    private static double averagePercent(double firstCurrent, double firstMax, double secondCurrent, double secondMax) {
        return (percent(firstCurrent, firstMax) + percent(secondCurrent, secondMax)) * 0.5D;
    }

    private static double percent(double current, double max) {
        if (!Double.isFinite(max) || max <= 0D) return 1D;
        return Math.max(0D, Math.min(1D, current / max));
    }

    private record PendingFusionResources(UUID leaderId, UUID partnerId, int ticksRemaining,
                                          double healthPercent, double energyPercent, double staminaPercent) {
        private PendingFusionResources nextTick() {
            return new PendingFusionResources(leaderId, partnerId, ticksRemaining - 1,
                    healthPercent, energyPercent, staminaPercent);
        }
    }

    private record FusionSyncState(long leaderBonuses, long partnerBonuses) {
    }
}
