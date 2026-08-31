package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.classes.skills.ClassSkillEvents;
import com.dmzrevamp.revamp.combat.SpdCombatScalingEvents;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.techniques.TechniqueDispatcher;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TechniqueDispatcher.class)
public abstract class TechniqueDispatcherSpdSpeedMixin {
    @Redirect(
            method = "executeKiAttack",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/stats/techniques/KiAttackData;getSpeed()F"),
            remap = false
    )
    private static float dmzrevamp$boostKiAttackSpeed(KiAttackData kiAttack, LivingEntity owner, Level level, KiAttackData data, StatsData statsData, float chargeMultiplier) {
        return (float) (kiAttack.getSpeed()
                * SpdCombatScalingEvents.getKiAttackSpeedMultiplier(statsData)
                * ClassSkillEvents.kiAssassinProjectileSpeedMultiplier(statsData, kiAttack));
    }
}
