package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.quest.RevampKillObjectiveData;
import com.dmzrevamp.revamp.quest.QuestSpawnAttributeApplier;
import com.dragonminez.common.config.RaidDefinition;
import com.dragonminez.common.quest.Difficulty;
import com.dragonminez.server.world.raid.Raid;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Raid.class, remap = false)
public abstract class RaidDefenseMixin {
    @Shadow @Final private Difficulty difficulty;

    @Inject(method = "applyStats", at = @At("RETURN"))
    private void dmzrevamp$applyDefense(Mob mob, RaidDefinition.Spawn spawn, CallbackInfo ci) {
        if (spawn instanceof RevampKillObjectiveData data) {
            QuestSpawnAttributeApplier.applyConfiguredEntityData(mob, data, spawn.canTransformOr(true),
                    1D, 1D, difficulty.damageMultiplier());
        }
    }

}
