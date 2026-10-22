package com.dmzrevamp.revamp.combat;

import com.dmzrevamp.DmzRevampMod;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.MeleeAnimationS2C;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.util.ComboManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Reinstates the old Alt + left-click combo on top of DMZ 2.2's ordinary melee path. */
@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LegacyComboManager {
    private static final String COOLDOWN_ID = "ComboAttackCooldown";
    private static final String PENDING_TAG = "dmzrevamp_legacy_combo_pending_until";
    private static final int COMBO_LENGTH = 4;
    // The old setting was removed from DMZ 2.2; retain its former short end-of-combo lockout.
    private static final int COMBO_COOLDOWN_TICKS = 100;

    private record Combo(int hit, int targetId) {
    }

    private static final Map<UUID, Combo> COMBOS = new ConcurrentHashMap<>();

    private LegacyComboManager() {
    }

    public static void arm(ServerPlayer player) {
        StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
        if (data == null || !data.getStatus().isHasCreatedCharacter() || data.getStatus().isStunned()
                || data.getStatus().isBlocking() || data.getCooldowns().hasCooldown(COOLDOWN_ID)) return;
        player.getPersistentData().putLong(PENDING_TAG, player.level().getGameTime() + 10L);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onOutgoingMelee(DMZEvent.DamageModifyEvent event) {
        if (event.getSourceType() != DMZEvent.DamageSourceType.MELEE
                || !(event.getAttacker() instanceof ServerPlayer attacker)) return;

        long pendingUntil = attacker.getPersistentData().getLong(PENDING_TAG);
        if (pendingUntil <= 0L) return;
        attacker.getPersistentData().remove(PENDING_TAG);

        StatsData data = StatsProvider.get(StatsCapability.INSTANCE, attacker).orElse(null);
        if (data == null || attacker.level().getGameTime() > pendingUntil
                || !data.getStatus().isHasCreatedCharacter() || data.getStatus().isStunned()
                || data.getStatus().isBlocking() || data.getCooldowns().hasCooldown(COOLDOWN_ID)
                || event.isCanceled()) {
            resetCombo(attacker, data, false);
            return;
        }

        // DMZ 2.0.4 only accepted a combo strike once the normal attack cooldown was nearly full.
        if (attacker.getAttackStrengthScale(0.5F) < 0.9F) {
            resetCombo(attacker, data, true);
            return;
        }

        LivingEntity target = event.getVictim();
        UUID id = attacker.getUUID();
        Combo previous = COMBOS.get(id);
        int hit = previous != null && previous.targetId() == target.getId()
                ? previous.hit() % COMBO_LENGTH + 1 : 1;

        // Replace the old Strike Damage source with the requested Melee Damage progression.
        event.setAmount(Math.max(0.0D, data.getMeleeDamage()) * (1.0D + hit * 0.05D));
        attacker.getPersistentData().putInt("dmzrevamp_legacy_combo_hit", hit);
        COMBOS.put(id, new Combo(hit, target.getId()));

        if (attacker.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            // DMZ 2.2 removed the old COMBO animation controller. Replay the same current
            // bare-hand attack animation for observers; the normal local attack animation
            // remains owned by DMZ's melee input pipeline.
            String animation = (hit % 2 == 0) ? "one_handed_punch_left" : "one_handed_punch_right";
            NetworkHandler.sendToTrackingEntity(new MeleeAnimationS2C(attacker.getId(), animation, false, 1.0F), attacker);
            serverLevel.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), MainSounds.CRITICO1.get(),
                    SoundSource.PLAYERS, 0.5F, 1.0F + hit * 0.1F);
        }

        if (hit == COMBO_LENGTH) {
            Vec3 look = attacker.getLookAngle();
            target.setDeltaMovement(look.x * 3.0D, 0.5D, look.z * 3.0D);
            target.hasImpulse = true;
            ComboManager.enableTeleportWindow(id, target.getId());
            target.level().playSound(null, target.getX(), target.getY(), target.getZ(), MainSounds.CRITICO2.get(),
                    SoundSource.PLAYERS, 0.8F, 1.0F);
            COMBOS.remove(id);
            data.getCooldowns().setCooldown(COOLDOWN_ID, COMBO_COOLDOWN_TICKS);
            if (attacker.level() instanceof net.minecraft.server.level.ServerLevel) {
                NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(attacker), attacker);
            }
        }
    }

    private static void resetCombo(ServerPlayer player, StatsData data, boolean punish) {
        COMBOS.remove(player.getUUID());
        player.getPersistentData().remove("dmzrevamp_legacy_combo_hit");
        if (!punish || data == null) return;
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 0.5F, 1.5F);
        data.getCooldowns().setCooldown(COOLDOWN_ID, COMBO_COOLDOWN_TICKS);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        COMBOS.remove(event.getEntity().getUUID());
        event.getEntity().getPersistentData().remove(PENDING_TAG);
        event.getEntity().getPersistentData().remove("dmzrevamp_legacy_combo_hit");
    }
}
