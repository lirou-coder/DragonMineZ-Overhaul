package com.dmzrevamp.revamp;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** Stores the Overhaul-only movement bonus limit, independently from DMZ's flight limit. */
public final class OverhaulSpeedLimitState {
    private static final String NBT_KEY = "dmzrevamp_overhaul_speed_limit";
    private static volatile int clientValue = 100;

    private OverhaulSpeedLimitState() {
    }

    public static int get(Player player) {
        if (player == null) return 100;
        if (player.level().isClientSide()) return clamp(clientValue);
        return clamp(player.getPersistentData().contains(NBT_KEY)
                ? player.getPersistentData().getInt(NBT_KEY) : 100);
    }

    public static int set(ServerPlayer player, int value) {
        int clamped = clamp(value);
        player.getPersistentData().putInt(NBT_KEY, clamped);
        return clamped;
    }

    public static void copy(Player from, Player to) {
        if (from.getPersistentData().contains(NBT_KEY)) {
            to.getPersistentData().putInt(NBT_KEY, clamp(from.getPersistentData().getInt(NBT_KEY)));
        }
    }

    public static void setClient(int value) {
        clientValue = clamp(value);
    }

    private static int clamp(int value) {
        return Math.max(5, Math.min(100, (value / 5) * 5));
    }
}
