package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.ki.KiAttackOverhaul;
import com.dmzrevamp.revamp.strike.StrikeAttackDelayManager;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Status;
import com.dragonminez.server.events.players.TickHandler;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.UUID;

@Mixin(TickHandler.class)
public abstract class TickHandlerAuraOverchargeMixin {
    @Redirect(
            method = "lambda$onPlayerTick$1",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/stats/character/Status;setAuraActive(Z)V"),
            remap = false
    )
    private static void dmzrevamp$keepAuraActiveDuringOvercharge(Status status, boolean auraActive, ServerPlayer player, int tick, UUID playerId, StatsData data) {
        if (StrikeAttackDelayManager.isPlayerDelaying(playerId)) {
            status.setAuraActive(true);
            return;
        }
        if (auraActive || data == null || data.getTechniques() == null || !data.getTechniques().isTechniqueChargeActive()) {
            status.setAuraActive(auraActive);
            return;
        }
        status.setAuraActive(KiAttackOverhaul.isVisuallyOverloaded(data.getTechniques().getTechniqueChargePercent()));
    }
}
