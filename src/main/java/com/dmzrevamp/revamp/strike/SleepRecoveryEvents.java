package com.dmzrevamp.revamp.strike;

import com.dmzrevamp.DmzRevampMod;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.network.S2C.TriggerAnimationS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SleepRecoveryEvents {
    public static final String LOCK_COOLDOWN = "DmzRevampSleepRecoveryLock";
    private static final Set<UUID> ACTIVE_SLEEP_RECOVERY = new HashSet<>();

    private SleepRecoveryEvents() {
    }

    public static void markActive(ServerPlayer player) {
        ACTIVE_SLEEP_RECOVERY.add(player.getUUID());
        FlyingStrikeYLock.begin(player);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            boolean active = data.getCooldowns().hasCooldown(LOCK_COOLDOWN);
            if (active) {
                player.setDeltaMovement(Vec3.ZERO);
                player.hurtMarked = true;
                return;
            }
            if (ACTIVE_SLEEP_RECOVERY.remove(player.getUUID()) && data.getStatus().isStrikeLocked()) {
                data.getStatus().setStrikeLocked(false);
                FlyingStrikeYLock.finish(player);
                NetworkHandler.sendToTrackingEntityAndSelf(new TriggerAnimationS2C(player.getUUID(), TriggerAnimationS2C.AnimationType.KI_ANIMATION_STOP, 0, player.getId(), ""), player);
                NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
            }
        });
    }
}
