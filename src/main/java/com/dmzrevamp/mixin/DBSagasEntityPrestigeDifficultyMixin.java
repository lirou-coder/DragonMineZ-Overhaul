package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.prestige.PrestigeDifficultyHelper;
import com.dmzrevamp.revamp.prestige.PrestigeSystem;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.dragonminez.common.quest.Difficulty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = DBSagasEntity.class, remap = false)
public abstract class DBSagasEntityPrestigeDifficultyMixin {
    @Redirect(
            method = "finishTransformationSpawn",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/quest/Difficulty;hpMultiplier()D"),
            require = 0
    )
    private double dmzrevamp$combinePrestigeTransformedHealth(Difficulty difficulty) {
        return PrestigeSystem.roundedDifficultyValue(difficulty.hpMultiplier()
                * PrestigeDifficultyHelper.statMultiplier((DBSagasEntity) (Object) this));
    }

    @Redirect(
            method = "finishTransformationSpawn",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/quest/Difficulty;damageMultiplier()D"),
            require = 0
    )
    private double dmzrevamp$combinePrestigeTransformedDamage(Difficulty difficulty) {
        return PrestigeSystem.roundedDifficultyValue(difficulty.damageMultiplier()
                * PrestigeDifficultyHelper.statMultiplier((DBSagasEntity) (Object) this));
    }
}
