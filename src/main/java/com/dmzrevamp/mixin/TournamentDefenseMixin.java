package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.quest.ConfiguredTournamentTransformData;
import com.dmzrevamp.revamp.quest.RevampKillObjectiveData;
import com.dmzrevamp.revamp.quest.QuestSpawnAttributeApplier;
import com.dragonminez.common.config.TournamentDefinition;
import com.dragonminez.server.world.tournament.Tournament;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Tournament.Manager.class, remap = false)
public abstract class TournamentDefenseMixin {
    @Inject(method = "applyStats", at = @At("RETURN"))
    private static void dmzrevamp$applyDefense(Mob mob, TournamentDefinition.Fighter fighter,
                                               int round, CallbackInfo ci) {
        if (fighter instanceof RevampKillObjectiveData data) {
            double scaling = Math.pow(fighter.perRoundScalingOr(1D), round);
            boolean canTransform = fighter instanceof ConfiguredTournamentTransformData transformData
                    && transformData.dmzrevamp$isTransformEnabled();
            QuestSpawnAttributeApplier.applyConfiguredEntityData(mob, data, canTransform,
                    scaling, scaling, scaling);
        }
    }
}
