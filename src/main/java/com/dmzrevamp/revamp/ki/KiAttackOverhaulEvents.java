package com.dmzrevamp.revamp.ki;

import com.dmzrevamp.DmzRevampMod;
import com.dmzrevamp.compat.DmzKiOverchargeCompat;
import com.dmzrevamp.network.DmzRevampNetwork;
import com.dmzrevamp.network.OverchargeScreenShakeS2CPacket;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.dragonminez.common.init.entities.ki.KiWaveEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.character.Status;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class KiAttackOverhaulEvents {
    private static final String BASE_SIZE_TAG = DmzRevampMod.MODID + "_base_ki_projectile_size";
    private static final String BASE_CAST_SIZE_TAG = DmzRevampMod.MODID + "_base_ki_wave_cast_size";
    public static final String OVERCHARGE_PERCENT_TAG = DmzRevampMod.MODID + "_overcharge_percent";
    private static final int RELEASE_SCALE_TICKS = 40;
    private static final int SHAKE_INTERVAL_TICKS = 8;
    private static final Map<UUID, AuraRestoreState> AURA_RESTORE_STATES = new ConcurrentHashMap<>();
    private static final Map<UUID, PendingReleaseScale> PENDING_RELEASE_SCALES = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> SHAKE_COOLDOWNS = new ConcurrentHashMap<>();

    // Forge calls the static event methods directly, so this event holder should not be instantiated.
    private KiAttackOverhaulEvents() {
    }

    @SubscribeEvent
    // Updates the size of actively charged ki projectiles while they overcharge past 175 percent.
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide() && event.player instanceof ServerPlayer player) {
            tickPendingReleaseScale(player);
        }
        if (DmzKiOverchargeCompat.isLoaded()) {
            return;
        }
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide() || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).resolve().orElse(null);
        if (data == null || !data.getTechniques().isTechniqueChargeActive()) {
            endOverchargeEffects(player, data);
            return;
        }

        float chargePercent = KiAttackOverhaul.clampChargePercent(data.getTechniques().getTechniqueChargePercent());
        if (!KiAttackOverhaul.isVisuallyOverloaded(chargePercent)) {
            endOverchargeEffects(player, data);
            return;
        }

        beginAuraIfNeeded(player, data);
        tickScreenShake(player, chargePercent);
        float chargeMultiplier = chargePercent / 100.0F;
        float sizeMultiplier = KiAttackOverhaul.projectileSizeMultiplier(chargeMultiplier);
        scaleChargingProjectiles(player, sizeMultiplier, chargePercent);
    }

    @SubscribeEvent
    public static void onKiAttackFire(com.dragonminez.common.events.DMZEvent.KiAttackFireEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        KiAttackData attack = event.getKiAttack();
        if (attack == null || attack.getId() == null || attack.getId().isEmpty()) {
            return;
        }
        float chargePercent = KiAttackOverhaul.clampChargePercent(event.getChargeMultiplier() * 100.0F);
        PENDING_RELEASE_SCALES.put(player.getUUID(), new PendingReleaseScale(attack.getId(), chargePercent, RELEASE_SCALE_TICKS));
        tickPendingReleaseScale(player);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void capOverchargeCooldown(com.dragonminez.common.events.DMZEvent.KiAttackFireEvent event) {
        if (event.getPlayer().getAbilities().instabuild) {
            return;
        }
        event.setCooldownTicks(KiAttackOverhaul.capOverchargeCooldownToNormalDmzRelease(
                event.getKiAttack(),
                event.getChargeMultiplier(),
                event.getCooldownTicks()
        ));
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID playerId = event.getEntity().getUUID();
        AURA_RESTORE_STATES.remove(playerId);
        PENDING_RELEASE_SCALES.remove(playerId);
        SHAKE_COOLDOWNS.remove(playerId);
    }

    // Applies the current overcharge size multiplier to nearby non-firing projectiles owned by the player.
    private static void scaleChargingProjectiles(ServerPlayer player, float sizeMultiplier, float chargePercent) {
        if (sizeMultiplier <= 1.0F) {
            return;
        }
        var searchArea = player.getBoundingBox().inflate(30.0D);
        for (AbstractKiProjectile projectile : KiProjectileIndex.snapshot(player.serverLevel())) {
            if (projectile.getOwner() != null
                    && projectile.getOwner().getUUID().equals(player.getUUID())
                    && !projectile.isFiring()
                    && projectile.getBoundingBox().intersects(searchArea)) {
                scaleProjectile(projectile, sizeMultiplier, chargePercent);
            }
        }
    }

    // Stores the original projectile size and reapplies the requested overcharge multiplier.
    private static void scaleProjectile(AbstractKiProjectile projectile, float sizeMultiplier, float chargePercent) {
        CompoundTag tag = projectile.getPersistentData();
        if (!tag.contains(BASE_SIZE_TAG)) {
            tag.putFloat(BASE_SIZE_TAG, projectile.getSize());
        }
        tag.putFloat(OVERCHARGE_PERCENT_TAG, KiAttackOverhaul.clampChargePercent(chargePercent));
        float baseSize = Math.max(0.01F, tag.getFloat(BASE_SIZE_TAG));
        projectile.setSize(baseSize * sizeMultiplier);
        // Waves render their charging body from castSize rather than size. Keeping
        // both values in step also makes their visible body and collision volume
        // agree once the projectile starts firing.
        if (projectile instanceof KiWaveEntity wave) {
            if (!tag.contains(BASE_CAST_SIZE_TAG)) {
                tag.putFloat(BASE_CAST_SIZE_TAG, wave.getCastSize());
            }
            float baseCastSize = Math.max(0.01F, tag.getFloat(BASE_CAST_SIZE_TAG));
            wave.setCastSize(baseCastSize * sizeMultiplier);
        }
    }

    private static void tickPendingReleaseScale(ServerPlayer player) {
        PendingReleaseScale pending = PENDING_RELEASE_SCALES.get(player.getUUID());
        if (pending == null) {
            return;
        }
        if (pending.ticksRemaining() <= 0) {
            PENDING_RELEASE_SCALES.remove(player.getUUID(), pending);
            return;
        }

        float sizeMultiplier = DmzKiOverchargeCompat.isLoaded() ? 1.0F
                : KiAttackOverhaul.projectileSizeMultiplier(pending.chargePercent() / 100.0F);
        var searchArea = player.getBoundingBox().inflate(64.0D);
        for (AbstractKiProjectile projectile : KiProjectileIndex.snapshot(player.serverLevel())) {
            if (projectile.getOwner() != null
                    && projectile.getOwner().getUUID().equals(player.getUUID())
                    && pending.techniqueId().equals(projectile.getTechniqueId())
                    && projectile.getBoundingBox().intersects(searchArea)) {
                scaleProjectile(projectile, sizeMultiplier, pending.chargePercent());
            }
        }

        PENDING_RELEASE_SCALES.put(player.getUUID(), pending.withTicksRemaining(pending.ticksRemaining() - 1));
    }

    private static void beginAuraIfNeeded(ServerPlayer player, StatsData data) {
        Status status = data.getStatus();
        AURA_RESTORE_STATES.computeIfAbsent(player.getUUID(), ignored -> new AuraRestoreState(status.isAuraActive()));
        if (!status.isAuraActive()) {
            status.setAuraActive(true);
            NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
        }
    }

    private static void endOverchargeEffects(ServerPlayer player, StatsData data) {
        AuraRestoreState auraState = AURA_RESTORE_STATES.remove(player.getUUID());
        if (auraState != null && data != null && data.getStatus().isAuraActive() != auraState.wasAuraActive()) {
            data.getStatus().setAuraActive(auraState.wasAuraActive());
            NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
        }
        SHAKE_COOLDOWNS.remove(player.getUUID());
    }

    private static void tickScreenShake(ServerPlayer player, float chargePercent) {
        int cooldown = SHAKE_COOLDOWNS.getOrDefault(player.getUUID(), 0);
        if (cooldown > 0) {
            SHAKE_COOLDOWNS.put(player.getUUID(), cooldown - 1);
            return;
        }
        float progress = KiAttackOverhaul.secondOverchargeFill(chargePercent);
        float intensity = 0.15F + (progress * 0.35F);
        DmzRevampNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OverchargeScreenShakeS2CPacket(intensity, 10));
        SHAKE_COOLDOWNS.put(player.getUUID(), SHAKE_INTERVAL_TICKS);
    }

    private record AuraRestoreState(boolean wasAuraActive) {
    }

    private record PendingReleaseScale(String techniqueId, float chargePercent, int ticksRemaining) {
        private PendingReleaseScale withTicksRemaining(int ticksRemaining) {
            return new PendingReleaseScale(techniqueId, chargePercent, ticksRemaining);
        }
    }
}
