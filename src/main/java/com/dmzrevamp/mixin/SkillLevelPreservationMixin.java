package com.dmzrevamp.mixin;

import com.dragonminez.common.stats.skills.Skill;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Prevents a runtime max-level refresh from destructively erasing learned progression. */
@Mixin(value = Skill.class, remap = false)
public abstract class SkillLevelPreservationMixin {
    @ModifyVariable(method = "setMaxLevel", at = @At("HEAD"), argsOnly = true)
    private int dmzrevamp$keepLearnedLevelWithinMaximum(int requestedMaxLevel) {
        int learnedLevel = ((Skill) (Object) this).getLevel();
        return Math.max(requestedMaxLevel, learnedLevel);
    }
}
