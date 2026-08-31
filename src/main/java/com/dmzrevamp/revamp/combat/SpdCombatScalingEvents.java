package com.dmzrevamp.revamp.combat;

import com.dmzrevamp.DmzRevampMod;
import com.dmzrevamp.config.DmzRevampConfig;
import com.dmzrevamp.revamp.DmzRevampHelper;
import com.dmzrevamp.revamp.growth.DynamicGrowthRevampEvents;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.TriggerAnimationS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Cooldowns;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
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

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void evadePlayerDamage(LivingHurtEvent event) {
        tryEvadePlayerDamage(event);
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

    public static void tryEvadePlayerDamage(LivingHurtEvent event) {
        if (!DmzRevampConfig.ENABLE_SPD_PLAYER_EVASION.get() || event.getAmount() <= 0F) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer target) || !(event.getSource().getEntity() instanceof ServerPlayer attacker)) {
            return;
        }
        if (target == attacker || target.level().isClientSide()) {
            return;
        }

        StatsData targetData = getStats(target);
        StatsData attackerData = getStats(attacker);
        if (targetData == null || attackerData == null) {
            return;
        }

        double attackerSpeed = Math.max(0D, DmzRevampHelper.getCurrentSpeedValue(attackerData));
        double targetSpeed = Math.max(0D, DmzRevampHelper.getCurrentSpeedValue(targetData));
        if (targetSpeed <= 0D || attackerSpeed <= 0D) {
            return;
        }

        double maxChance = DmzRevampConfig.SPD_PLAYER_EVASION_MAX_CHANCE_PERCENT.get() / 100D;
        double ratio = targetSpeed / attackerSpeed;
        double chance = Math.max(0D, Math.min(maxChance, ((ratio - 5D) / 15D) * maxChance));
        if (chance <= 0D || target.getRandom().nextDouble() >= chance) {
            return;
        }

        event.setAmount(0F);
        event.setCanceled(true);
        target.invulnerableTime = Math.max(target.invulnerableTime, 10);
        target.hurtTime = 0;
        target.hurtDuration = 0;
        playEvasionFeedback(target);
        DynamicGrowthRevampEvents.awardPerfectDodge(target, attacker);
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

    private static void playEvasionFeedback(ServerPlayer target) {
        int variant = target.getRandom().nextInt(2);
        NetworkHandler.sendToTrackingEntityAndSelf(
                new TriggerAnimationS2C(target.getUUID(), TriggerAnimationS2C.AnimationType.EVASION, variant, target.getId()),
                target
        );
        SoundEvent sound = (variant == 0 ? MainSounds.EVASION1 : MainSounds.EVASION2).get();
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private record PendingCooldownReduction(UUID playerId, String cooldownKey, double reduction, int ticksRemaining) {
        private PendingCooldownReduction withTicksRemaining(int ticksRemaining) {
            return new PendingCooldownReduction(playerId, cooldownKey, reduction, ticksRemaining);
        }
    }
}
