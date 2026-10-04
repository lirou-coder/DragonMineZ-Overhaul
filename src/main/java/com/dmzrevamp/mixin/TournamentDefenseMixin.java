package com.dmzrevamp.mixin;

import com.dmzrevamp.entity.DmzRevampAttributes;
import com.dmzrevamp.revamp.quest.ConfiguredDefenseData;
import com.dmzrevamp.revamp.quest.QuestSpawnAttributeApplier;
import com.dragonminez.common.config.TournamentDefinition;
import com.dragonminez.server.world.tournament.Tournament;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Tournament.Manager.class, remap = false)
public abstract class TournamentDefenseMixin {
    @Inject(method = "applyStats", at = @At("RETURN"))
    private static void dmzrevamp$applyDefense(Mob mob, TournamentDefinition.Fighter fighter,
                                               int round, CallbackInfo ci) {
        if (!(fighter instanceof ConfiguredDefenseData data) || data.dmzrevamp$getDefense() == null) return;
        AttributeInstance defense = mob.getAttribute(DmzRevampAttributes.MOB_DEFENSE.get());
        if (defense != null) {
            double scaling = Math.pow(fighter.perRoundScalingOr(1D), round);
            double value = Math.max(0D, data.dmzrevamp$getDefense()) * scaling;
            mob.getPersistentData().putDouble(QuestSpawnAttributeApplier.DEFENSE_TAG, value);
            defense.setBaseValue(value);
        }
    }
}
