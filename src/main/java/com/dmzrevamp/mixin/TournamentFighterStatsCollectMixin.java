package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.quest.RevampKillObjectiveData;
import com.dmzrevamp.revamp.quest.TournamentFighterStatsExtension;
import com.dragonminez.common.config.TournamentDefinition;
import com.dragonminez.common.network.TournamentPackets;
import com.dragonminez.server.world.tournament.Tournament;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

@Mixin(value = Tournament.Manager.class, remap = false)
public abstract class TournamentFighterStatsCollectMixin {
    @Redirect(method = "putStats",
            at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"),
            require = 0)
    private static Object dmzrevamp$includeConfiguredDefenseAndSpeed(
            Map<String, TournamentPackets.OpenBracketS2C.FighterStats> stats,
            Object id,
            Object rawStats,
            Map<String, TournamentPackets.OpenBracketS2C.FighterStats> originalStats,
            String slot,
            TournamentDefinition.Fighter fighter,
            int round) {
        if (rawStats instanceof TournamentPackets.OpenBracketS2C.FighterStats fighterStats
                && fighter instanceof RevampKillObjectiveData configured) {
            TournamentFighterStatsExtension extra = (TournamentFighterStatsExtension) (Object) fighterStats;
            double scaling = Math.pow(fighter.perRoundScalingOr(1D), round);
            Double defense = configured.dmzrevamp$getDefense();
            Double movementSpeed = configured.dmzrevamp$getMovementSpeed();
            extra.dmzrevamp$setDefense(defense == null ? null : defense * scaling);
            extra.dmzrevamp$setMovementSpeed(movementSpeed);
        }
        return stats.put((String) id, (TournamentPackets.OpenBracketS2C.FighterStats) rawStats);
    }
}
