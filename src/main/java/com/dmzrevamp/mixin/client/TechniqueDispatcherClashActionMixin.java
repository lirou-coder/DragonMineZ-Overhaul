package com.dmzrevamp.mixin.client;

import com.dmzrevamp.config.KiClashConfigured;
import com.dragonminez.client.clash.ClientBeamClashState;
import com.dragonminez.client.events.ClientStatsEvents;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.techniques.TechniqueDispatcher;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Neutralizes DMZ's ACTION/ACTION_CHARGE restriction for the full clash lifetime. */
@Mixin(ClientStatsEvents.class)
public abstract class TechniqueDispatcherClashActionMixin {
    @Redirect(method = "lambda$onClientTick$3", at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/stats/techniques/TechniqueDispatcher;isActionRestrictedKiAttack(Lnet/minecraft/world/entity/player/Player;Lcom/dragonminez/common/stats/StatsData;)Z"), remap = false)
    private static boolean dmzrevamp$neverRestrictActionDuringClash(Player player, StatsData data) {
        if (ClientBeamClashState.isActive() && KiClashConfigured.get().allowTransformationMidClash) {
            return false;
        }
        return TechniqueDispatcher.isActionRestrictedKiAttack(player, data);
    }
}
