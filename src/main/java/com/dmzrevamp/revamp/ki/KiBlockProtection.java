package com.dmzrevamp.revamp.ki;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

public final class KiBlockProtection {
    private KiBlockProtection() {
    }

    public static boolean isPlayerSupportArea(ServerPlayer player, BlockPos pos) {
        if (player == null || pos == null) {
            return false;
        }
        BlockPos center = BlockPos.containing(player.getX(), player.getY() - 1.0D, player.getZ());
        return pos.getY() == center.getY()
                && Math.abs(pos.getX() - center.getX()) <= 1
                && Math.abs(pos.getZ() - center.getZ()) <= 1;
    }
}
