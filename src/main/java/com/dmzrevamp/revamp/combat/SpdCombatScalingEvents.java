package com.dmzrevamp.revamp.combat;

import com.dmzrevamp.DmzRevampMod;
import com.dmzrevamp.revamp.DmzRevampHelper;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Cooldowns;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SpdCombatScalingEvents {
    private static final Map<UUID, Map<String, PendingCooldownReduction>> PENDING_COOLDOWN_REDUCTIONS = new ConcurrentHashMap<>();

    private SpdCombatScalingEvents() {
    }

    @SubscribeEvent
    public static void reduceKiAttackCooldown(DMZEvent.KiAttackFireEvent event) {
        event.setCooldownTicks(reduceTicks(event.getCooldownTicks(), DmzRevampHelper.getSpdCooldownReduction(event.getStatsData())));
    }

    @SubscribeEvent
    public static void rememberStrikeCooldownReduction(DMZEvent.StrikeAttackCastEvent event) {
        double speedReduction = DmzRevampHelper.getSpdCooldownReduction(event.getStatsData());
        double passiveReduction = com.dmzrevamp.revamp.classes.skills.CustomClassPassiveEvents
                .strikeCooldownReduction(event.getStatsData());
        double reduction = 1D - (1D - speedReduction) * (1D - passiveReduction);
        if (reduction <= 0D || event.getStrike() == null || event.getStrike().getId() == null || event.getStrike().getId().isEmpty()) {
            return;
        }
        String cooldownKey = getTechniqueCooldownKey(event.getStrike().getId());
        rememberPendingCooldownReduction(
                event.getPlayer().getUUID(),
                cooldownKey,
                reduction,
                1200
        );
    }

    @SubscribeEvent
    public static void rememberDashCooldownReduction(DMZEvent.PlayerDashEvent event) {
        ServerPlayer player = event.getPlayer();
        double reduction = getCooldownReduction(player);
        if (reduction <= 0D) {
            return;
        }
        rememberPendingCooldownReduction(player.getUUID(), Cooldowns.DASH_CD, reduction, 120);
        if (event.getDashType() == DMZEvent.PlayerDashEvent.DashType.DOUBLE) {
            rememberPendingCooldownReduction(player.getUUID(), Cooldowns.DOUBLEDASH_CD, reduction, 120);
        }
    }

    private static void rememberPendingCooldownReduction(UUID playerId, String cooldownKey, double reduction, int ticksRemaining) {
        PENDING_COOLDOWN_REDUCTIONS
                .computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>())
                .put(cooldownKey, new PendingCooldownReduction(playerId, cooldownKey, reduction, ticksRemaining));
    }

    @SubscribeEvent
    public static void reducePendingCooldowns(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide() || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            UUID playerId = player.getUUID();
            Map<String, PendingCooldownReduction> pendingCooldowns = PENDING_COOLDOWN_REDUCTIONS.get(playerId);
            if (pendingCooldowns == null || pendingCooldowns.isEmpty()) {
                return;
            }
            for (Map.Entry<String, PendingCooldownReduction> entry : pendingCooldowns.entrySet()) {
                String key = entry.getKey();
                PendingCooldownReduction pending = entry.getValue();
                int currentCooldown = data.getCooldowns().getCooldown(pending.cooldownKey());
                if (currentCooldown > 0) {
                    int reducedTicks = reduceTicks(currentCooldown, pending.reduction());
                    data.getCooldowns().setCooldown(pending.cooldownKey(), reducedTicks);
                    syncDashCooldownEffect(player, pending.cooldownKey(), reducedTicks);
                    pendingCooldowns.remove(key, pending);
                    continue;
                }
                PendingCooldownReduction aged = pending.withTicksRemaining(pending.ticksRemaining() - 1);
                if (aged.ticksRemaining() <= 0) {
                    pendingCooldowns.remove(key, pending);
                } else {
                    pendingCooldowns.replace(key, pending, aged);
                }
            }
            if (pendingCooldowns.isEmpty()) {
                PENDING_COOLDOWN_REDUCTIONS.remove(playerId, pendingCooldowns);
            }
        });
    }

    public static int reduceTicks(int ticks, double reduction) {
        if (ticks <= 1 || reduction <= 0D) {
            return ticks;
        }
        double clampedReduction = Math.max(0D, Math.min(1D, reduction));
        return Math.max(1, (int) Math.ceil(ticks * (1D - clampedReduction)));
    }

    public static double getCooldownReduction(Player player) {
        StatsData data = getStats(player);
        return data == null ? 0D : DmzRevampHelper.getSpdCooldownReduction(data);
    }

    public static double getAttackSpeedIncrease(Player player) {
        StatsData data = getStats(player);
        return data == null ? 0D : DmzRevampHelper.getSpdAttackSpeedIncrease(data);
    }

    public static double getKiAttackSpeedMultiplier(StatsData data) {
        return 1D + DmzRevampHelper.getSpdAttackSpeedIncrease(data);
    }

    public static double getStrikeDashDistanceMultiplier(ServerPlayer player) {
        return 1D + getAttackSpeedIncrease(player);
    }

    private static StatsData getStats(Player player) {
        if (player == null) {
            return null;
        }
        return StatsProvider.get(StatsCapability.INSTANCE, player)
                .filter(data -> data.getStatus().isHasCreatedCharacter())
                .orElse(null);
    }

    private static String getTechniqueCooldownKey(String techniqueId) {
        return "TechniqueCooldown_" + techniqueId;
    }

    private static void syncDashCooldownEffect(ServerPlayer player, String cooldownKey, int ticks) {
        if (Cooldowns.DASH_CD.equals(cooldownKey)) {
            player.addEffect(new MobEffectInstance(MainEffects.DASH_CD.get(), ticks, 0, false, false, true));
        } else if (Cooldowns.DOUBLEDASH_CD.equals(cooldownKey)) {
            player.addEffect(new MobEffectInstance(MainEffects.DOUBLEDASH_CD.get(), ticks, 0, false, false, true));
        }
    }

    private record PendingCooldownReduction(UUID playerId, String cooldownKey, double reduction, int ticksRemaining) {
        private PendingCooldownReduction withTicksRemaining(int ticksRemaining) {
            return new PendingCooldownReduction(playerId, cooldownKey, reduction, ticksRemaining);
        }
    }
}
