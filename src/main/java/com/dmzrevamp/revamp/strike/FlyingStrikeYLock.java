package com.dmzrevamp.revamp.strike;

import com.dmzrevamp.DmzRevampMod;
import com.dmzrevamp.network.DmzRevampNetwork;
import com.dmzrevamp.network.StrikeYAnchorS2CPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.Map;
import java.util.WeakHashMap;

/** Locks only Y during a strike and for ten ticks afterward; X/Z movement remains untouched. */
@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FlyingStrikeYLock {
    private static final int POST_STRIKE_TICKS = 10;
    private static final int SCRIPTED_VERTICAL_MOTION = -2;
    private static final Map<Player, State> STATES = new WeakHashMap<>();

    private FlyingStrikeYLock() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        synchronized (FlyingStrikeYLock.class) {
            State state = STATES.get(player);
            if (state == null) {
                return;
            }
            if (!state.strikeActive && player.tickCount >= state.lockUntilTick) {
                STATES.remove(player);
                return;
            }
            if (player.tickCount <= state.allowVerticalUntilTick) {
                if (Math.abs(player.getY() - state.anchorY) > 1.0E-7D) {
                    state.anchorY = player.getY();
                    if (player instanceof ServerPlayer serverPlayer) {
                        syncAnchor(serverPlayer, state.anchorY, SCRIPTED_VERTICAL_MOTION);
                    }
                }
                player.fallDistance = 0.0F;
                return;
            }
            enforceAnchor(player, state.anchorY);
        }
    }

    /** Starts the lock for the attacker only; DMZ also sets strikeLocked on victims. */
    public static synchronized void begin(ServerPlayer player) {
        State state = STATES.get(player);
        if (state == null || !state.strikeActive) {
            state = new State(player.getY());
            STATES.put(player, state);
        }
        state.anchorY = player.getY();
        state.strikeActive = true;
        state.lockUntilTick = Integer.MAX_VALUE;
        enforceAnchor(player, state.anchorY);
        syncAnchor(player, state.anchorY, -1);
    }

    public static synchronized void finish(ServerPlayer player) {
        State state = STATES.get(player);
        if (state == null) {
            state = new State(player.getY());
            STATES.put(player, state);
        }
        state.strikeActive = false;
        state.anchorY = player.getY();
        state.allowVerticalUntilTick = Integer.MIN_VALUE;
        state.lockUntilTick = player.tickCount + POST_STRIKE_TICKS;
        enforceAnchor(player, state.anchorY);
        syncAnchor(player, state.anchorY, POST_STRIKE_TICKS);
    }

    /** Called immediately after a strike-owned teleport, before the tick lock can restore old Y. */
    public static synchronized void updateAnchorAfterStrikeTeleport(ServerPlayer player) {
        State state = STATES.get(player);
        if (state == null || !state.strikeActive) {
            return;
        }
        state.anchorY = player.getY();
        state.lockUntilTick = Integer.MAX_VALUE;
        zeroVerticalMotion(player);
        syncAnchor(player, state.anchorY, -1);
    }

    /** Lets a strike-owned dash change Y, then adopts the resulting position as the new lock anchor. */
    public static synchronized void allowScriptedVerticalMotion(ServerPlayer player) {
        State state = STATES.get(player);
        if (state == null || !state.strikeActive) {
            return;
        }
        state.allowVerticalUntilTick = Math.max(state.allowVerticalUntilTick, player.tickCount + 2);
        syncAnchor(player, player.getY(), SCRIPTED_VERTICAL_MOTION);
    }

    /** Moves a strike charge immediately so flight controllers cannot erase its velocity first. */
    public static synchronized void applyScriptedHorizontalDisplacement(Player player, Vec3 displacement) {
        State state = STATES.get(player);
        if (state == null || !state.strikeActive || displacement.horizontalDistanceSqr() < 1.0E-10D) {
            player.setDeltaMovement(displacement);
            return;
        }
        Vec3 horizontal = new Vec3(displacement.x, 0.0D, displacement.z);
        player.move(MoverType.SELF, horizontal);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        player.hurtMarked = true;
        state.anchorY = player.getY();
    }

    public static synchronized boolean isLocked(Player player) {
        return STATES.containsKey(player);
    }

    /** Applies the authoritative server anchor to the local player after a strike teleport. */
    public static synchronized void syncClientAnchor(Player player, double anchorY, int remainingTicks) {
        State state = STATES.computeIfAbsent(player, ignored -> new State(anchorY));
        if (remainingTicks == SCRIPTED_VERTICAL_MOTION) {
            state.anchorY = player.getY();
            state.strikeActive = true;
            state.lockUntilTick = Integer.MAX_VALUE;
            state.allowVerticalUntilTick = Math.max(state.allowVerticalUntilTick, player.tickCount + 2);
            return;
        }
        state.anchorY = anchorY;
        state.strikeActive = remainingTicks < 0;
        state.lockUntilTick = remainingTicks < 0 ? Integer.MAX_VALUE : player.tickCount + remainingTicks;
        enforceAnchor(player, anchorY);
    }

    @SubscribeEvent
    public static synchronized void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        STATES.remove(event.getEntity());
    }

    private static void enforceAnchor(Player player, double anchorY) {
        if (Math.abs(player.getY() - anchorY) > 1.0E-7D) {
            player.setPos(player.getX(), anchorY, player.getZ());
            player.hurtMarked = true;
        }
        zeroVerticalMotion(player);
        player.fallDistance = 0.0F;
    }

    private static void zeroVerticalMotion(Player player) {
        Vec3 movement = player.getDeltaMovement();
        if (Math.abs(movement.y) > 1.0E-9D) {
            player.setDeltaMovement(movement.x, 0.0D, movement.z);
        }
    }

    private static void syncAnchor(Player player, double anchorY, int remainingTicks) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        DmzRevampNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> serverPlayer),
                new StrikeYAnchorS2CPacket(anchorY, remainingTicks)
        );
    }

    private static final class State {
        private double anchorY;
        private boolean strikeActive;
        private int lockUntilTick = Integer.MIN_VALUE;
        private int allowVerticalUntilTick = Integer.MIN_VALUE;

        private State(double anchorY) {
            this.anchorY = anchorY;
        }
    }
}
