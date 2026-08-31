package com.dmzrevamp.revamp.ki;

import com.dmzrevamp.config.KiClashConfigured;
import com.dragonminez.common.combat.clash.BeamClashManager;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.extras.ActionMode;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-authoritative ACTION state used only for charged transformations during a clash. */
public final class KiClashTransformationBridge {
    private static final long INPUT_GRACE_TICKS = 5L;
    private static final Map<UUID, Long> HELD_UNTIL = new ConcurrentHashMap<>();

    private KiClashTransformationBridge() {}

    public static void updateInput(ServerPlayer player, boolean held) {
        UUID playerId = player.getUUID();
        if (!held || !KiClashConfigured.get().allowTransformationMidClash || !isInClash(playerId)) {
            HELD_UNTIL.remove(playerId);
            return;
        }
        HELD_UNTIL.put(playerId, player.level().getGameTime() + INPUT_GRACE_TICKS);
    }

    public static void restoreActionCharge(ServerPlayer player, StatsData data) {
        if (!isChargeHeld(player, data)) return;
        data.getStatus().setActionCharging(true);
    }

    private static boolean isChargeHeld(ServerPlayer player, StatsData data) {
        if (!KiClashConfigured.get().allowTransformationMidClash || !isInClash(player.getUUID())) return false;
        Long heldUntil = HELD_UNTIL.get(player.getUUID());
        if (heldUntil == null || heldUntil < player.level().getGameTime()) {
            HELD_UNTIL.remove(player.getUUID());
            return false;
        }
        ActionMode mode = data.getStatus().getSelectedAction();
        return mode == ActionMode.FORM || mode == ActionMode.STACK;
    }

    private static boolean isInClash(UUID playerId) {
        return BeamClashManager.isClashing(playerId) || KiClashTeams.isHelper(playerId);
    }
}
