package com.dmzrevamp.revamp.strike;

import com.dmzrevamp.DmzRevampMod;
import com.dmzrevamp.config.StrikeClashConfigured;
import com.dmzrevamp.mixin.StrikeAttackHandlerStateAccessor;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.network.S2C.TriggerAnimationS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Status;
import com.dragonminez.common.stats.techniques.StrikeAttackData;
import com.dragonminez.common.stats.techniques.TechniqueData;
import com.dragonminez.server.events.players.combat.StrikeAttackHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Telegraph phase shared by player Strike Attacks and saga-NPC Combo Attacks. */
@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class StrikeAttackDelayManager {
    private static final String CHARGE_ANIMATION = "base.ki_charge";
    private static final Map<UUID, PlayerDelay> PLAYER_DELAYS = new HashMap<>();
    private static final Map<UUID, NpcDelay> NPC_DELAYS = new HashMap<>();
    private static final Set<UUID> PLAYER_BYPASS = ConcurrentHashMap.newKeySet();
    private static final ThreadLocal<Boolean> EXECUTING_PRESERVED_DELAY =
            ThreadLocal.withInitial(() -> false);

    private StrikeAttackDelayManager() {
    }

    /** Called before DMZ processes a player request. Returns true while that request is delayed. */
    public static boolean interceptPlayerRequest(ServerPlayer player, int preferredTargetId) {
        UUID id = player.getUUID();
        if (PLAYER_BYPASS.remove(id)) return false;

        StrikeClashConfigured.Config config = StrikeClashConfigured.get();
        if (!config.strikeAttackHasDelay || config.strikeAttackDelayTicks <= 0) return false;
        if (PLAYER_DELAYS.containsKey(id)) return true;
        if (StrikeAttackHandlerStateAccessor.dmzrevamp$getActiveStrikes().containsKey(id)
                || StrikeAttackHandlerStateAccessor.dmzrevamp$getPendingStrikes().containsKey(id)) {
            return false;
        }

        StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).resolve().orElse(null);
        if (data == null || !data.getStatus().isHasCreatedCharacter() || data.getStatus().isStunned()) return false;
        TechniqueData selected = data.getTechniques().getSelectedTechnique();
        if (!(selected instanceof StrikeAttackData strike)) return false;

        String cooldownKey = "TechniqueCooldown_" + strike.getId();
        if (data.getCooldowns().hasCooldown(cooldownKey)) return false;
        double cost = strike.getCalculatedCost(data);
        boolean creativeCustomStrike = player.getAbilities().instabuild
                && strike instanceof RevampStrikeAttackData revamp
                && revamp.dmzrevamp$isCustomStrike();
        if (!creativeCustomStrike && data.getResources().getCurrentEnergy() < cost) return false;

        boolean racialRecovery = StrikeAttackTemplates.SLEEP_RECOVERY.equals(strike.getId())
                || StrikeAttackTemplates.NAMEKIAN_REGENERATION.equals(strike.getId());
        if (!racialRecovery && (data.getSkills().getSkillLevel("kicontrol") <= 0
                || data.getResources().getPowerRelease() < 5)) {
            return false;
        }

        PlayerDelay delay = new PlayerDelay(player, preferredTargetId, strike.getId(),
                config.strikeAttackDelayTicks, player.position(), data.getStatus().isAuraActive());
        PLAYER_DELAYS.put(id, delay);
        data.getStatus().setAuraActive(true);
        syncStats(player);
        playChargeAnimation(player);
        freeze(player, delay.anchor);
        return true;
    }

    /** Redirect target for DBSagasEntity.startCombo's validated setComboing(true). */
    public static void beginNpcCombo(DBSagasEntity npc) {
        StrikeClashConfigured.Config config = StrikeClashConfigured.get();
        if (!config.strikeAttackHasDelay || config.strikeAttackDelayTicks <= 0) {
            npc.setComboing(true);
            return;
        }
        NpcDelay existing = NPC_DELAYS.get(npc.getUUID());
        if (existing != null) return;

        NpcDelay delay = new NpcDelay(npc, config.strikeAttackDelayTicks, npc.position(), npc.isCharge());
        NPC_DELAYS.put(npc.getUUID(), delay);
        npc.getNavigation().stop();
        npc.setKiCharge(true);
        freeze(npc, delay.anchor);
    }

    public static boolean isPlayerDelaying(UUID playerId) {
        return playerId != null && PLAYER_DELAYS.containsKey(playerId);
    }

    /**
     * DMZ folds the victim strike lock into Status#isStunned. A request that was
     * already telegraphed before that lock must still be allowed to fire so its
     * first hit can open a Strike Clash. Real stun and knockdown still interrupt it.
     */
    public static boolean isBlockedDuringRequest(Status status) {
        if (status == null) return true;
        if (Boolean.TRUE.equals(EXECUTING_PRESERVED_DELAY.get())) {
            return status.isStunEffect() || status.isKnockedDown();
        }
        return status.isStunned();
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()
                || !(event.player instanceof ServerPlayer player)) return;
        PlayerDelay delay = PLAYER_DELAYS.get(player.getUUID());
        if (delay == null) return;

        if (!player.isAlive() || player.isRemoved()) {
            cancelPlayer(delay);
            return;
        }

        StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).resolve().orElse(null);
        if (data == null || data.getStatus().isStunEffect() || data.getStatus().isKnockedDown()
                || !sameSelectedStrike(data, delay.techniqueId)) {
            cancelPlayer(delay);
            return;
        }

        freeze(player, delay.anchor);
        if (!data.getStatus().isAuraActive()) {
            data.getStatus().setAuraActive(true);
            syncStats(player);
        }

        delay.ticksRemaining--;
        if (!StrikeClashConfigured.get().strikeAttackHasDelay || delay.ticksRemaining <= 0) {
            PLAYER_DELAYS.remove(player.getUUID());
            restorePlayerVisuals(delay, data);
            PLAYER_BYPASS.add(player.getUUID());
            EXECUTING_PRESERVED_DELAY.set(true);
            try {
                StrikeAttackHandler.requestStrike(player, delay.preferredTargetId);
            } finally {
                EXECUTING_PRESERVED_DELAY.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        Iterator<NpcDelay> iterator = NPC_DELAYS.values().iterator();
        while (iterator.hasNext()) {
            NpcDelay delay = iterator.next();
            DBSagasEntity npc = delay.npc;
            if (npc.level() != level) continue;
            if (!npc.isAlive() || npc.isRemoved() || npc.isStunned() || npc.getTarget() == null) {
                npc.setKiCharge(delay.wasCharging);
                iterator.remove();
                continue;
            }

            npc.getNavigation().stop();
            freeze(npc, delay.anchor);
            if (!npc.isCharge()) npc.setKiCharge(true);
            if (npc.getTarget() != null) npc.lookAt(npc.getTarget(), 360F, 360F);

            delay.ticksRemaining--;
            if (!StrikeClashConfigured.get().strikeAttackHasDelay || delay.ticksRemaining <= 0) {
                npc.setKiCharge(delay.wasCharging);
                npc.setComboing(true);
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PlayerDelay delay = PLAYER_DELAYS.remove(player.getUUID());
        PLAYER_BYPASS.remove(player.getUUID());
        if (delay != null) {
            StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).resolve().orElse(null);
            restorePlayerVisuals(delay, data);
        }
    }

    private static void cancelPlayer(PlayerDelay delay) {
        PLAYER_DELAYS.remove(delay.player.getUUID());
        StatsData data = StatsProvider.get(StatsCapability.INSTANCE, delay.player).resolve().orElse(null);
        restorePlayerVisuals(delay, data);
    }

    private static boolean sameSelectedStrike(StatsData data, String techniqueId) {
        TechniqueData current = data.getTechniques().getSelectedTechnique();
        return current instanceof StrikeAttackData strike && techniqueId.equals(strike.getId());
    }

    private static void restorePlayerVisuals(PlayerDelay delay, StatsData data) {
        if (data != null) {
            data.getStatus().setAuraActive(delay.wasAuraActive);
            syncStats(delay.player);
        }
        NetworkHandler.sendToTrackingEntityAndSelf(new TriggerAnimationS2C(
                delay.player.getUUID(), TriggerAnimationS2C.AnimationType.KI_ANIMATION_STOP,
                0, -1, ""), delay.player);
    }

    private static void playChargeAnimation(ServerPlayer player) {
        NetworkHandler.sendToTrackingEntityAndSelf(new TriggerAnimationS2C(
                player.getUUID(), TriggerAnimationS2C.AnimationType.KI_ANIMATION,
                1, -1, CHARGE_ANIMATION), player);
    }

    private static void syncStats(ServerPlayer player) {
        NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
    }

    private static void freeze(net.minecraft.world.entity.LivingEntity entity, Vec3 anchor) {
        if (entity instanceof ServerPlayer player) player.teleportTo(anchor.x, anchor.y, anchor.z);
        else entity.setPos(anchor.x, anchor.y, anchor.z);
        entity.setDeltaMovement(Vec3.ZERO);
        entity.hurtMarked = true;
        entity.fallDistance = 0F;
    }

    private static final class PlayerDelay {
        private final ServerPlayer player;
        private final int preferredTargetId;
        private final String techniqueId;
        private final Vec3 anchor;
        private final boolean wasAuraActive;
        private int ticksRemaining;

        private PlayerDelay(ServerPlayer player, int preferredTargetId, String techniqueId,
                            int ticksRemaining, Vec3 anchor, boolean wasAuraActive) {
            this.player = player;
            this.preferredTargetId = preferredTargetId;
            this.techniqueId = techniqueId;
            this.ticksRemaining = ticksRemaining;
            this.anchor = anchor;
            this.wasAuraActive = wasAuraActive;
        }
    }

    private static final class NpcDelay {
        private final DBSagasEntity npc;
        private final Vec3 anchor;
        private final boolean wasCharging;
        private int ticksRemaining;

        private NpcDelay(DBSagasEntity npc, int ticksRemaining, Vec3 anchor, boolean wasCharging) {
            this.npc = npc;
            this.ticksRemaining = ticksRemaining;
            this.anchor = anchor;
            this.wasCharging = wasCharging;
        }
    }
}
