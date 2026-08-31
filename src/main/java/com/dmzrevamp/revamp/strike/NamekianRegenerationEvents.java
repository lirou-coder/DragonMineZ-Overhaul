package com.dmzrevamp.revamp.strike;

import com.dmzrevamp.DmzRevampMod;
import com.dmzrevamp.sound.DmzRevampSounds;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.network.S2C.TriggerAnimationS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class NamekianRegenerationEvents {
    private static final Map<UUID, Integer> ACTIVE = new HashMap<>();

    private NamekianRegenerationEvents() {
    }

    public static void start(ServerPlayer player) {
        ACTIVE.put(player.getUUID(), 0);
        FlyingStrikeYLock.begin(player);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        Integer previous = ACTIVE.get(player.getUUID());
        if (previous == null) return;

        int tick = previous + 1;
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            player.setDeltaMovement(Vec3.ZERO);
            player.hurtMarked = true;
            if (tick == 3) playFromUser(player, DmzRevampSounds.GRAB.get());
            if (tick == 8) playFromUser(player, DmzRevampSounds.ARM_SNAP.get());
            if (tick == 17) {
                playFromUser(player, DmzRevampSounds.ARM_REGEN.get());
                float missingHealth = Math.max(0F, player.getMaxHealth() - player.getHealth());
                float requestedHeal = player.getMaxHealth() * 0.20F;
                float restored = Math.min(Math.min(requestedHeal, missingHealth), data.getResources().getCurrentStamina());
                if (restored > 0F) {
                    data.getResources().removeStamina(restored);
                    player.heal(restored);
                    NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
                }
            }
            if (tick >= 28 || !player.isAlive()) {
                finish(player, data);
            } else {
                ACTIVE.put(player.getUUID(), tick);
            }
        });
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ACTIVE.remove(event.getEntity().getUUID());
    }

    private static void finish(ServerPlayer player, com.dragonminez.common.stats.StatsData data) {
        ACTIVE.remove(player.getUUID());
        FlyingStrikeYLock.finish(player);
        data.getStatus().setStrikeLocked(false);
        NetworkHandler.sendToTrackingEntityAndSelf(new TriggerAnimationS2C(
                player.getUUID(), TriggerAnimationS2C.AnimationType.KI_ANIMATION_STOP, 0, player.getId(), ""), player);
        NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
    }

    private static void playFromUser(ServerPlayer player, SoundEvent sound) {
        // A null excluded-player broadcasts to the user as well as every nearby
        // observer while keeping the sound spatially attached to the caster.
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                sound, SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}
