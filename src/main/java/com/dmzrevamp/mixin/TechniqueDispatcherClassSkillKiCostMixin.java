package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.classes.skills.ClassSkillEvents;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Resources;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.techniques.TechniqueDispatcher;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TechniqueDispatcher.class)
public abstract class TechniqueDispatcherClassSkillKiCostMixin {
    @Redirect(
            method = "executeKiAttack",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/stats/techniques/KiAttackData;getCalculatedCost(Lcom/dragonminez/common/stats/StatsData;)D"),
            require = 0,
            remap = false
    )
    // Handles the adjustTechniqueKiCost logic for this class.
    private static double dmzrevamp$adjustTechniqueKiCost(KiAttackData technique, StatsData data, LivingEntity caster, Level level, KiAttackData methodTechnique, StatsData methodData, float chargePercent) {
        double originalCost = technique.getCalculatedCost(data);
        if (!(caster instanceof ServerPlayer player)) {
            return originalCost;
        }
        return ClassSkillEvents.adjustKiActionCost(player, data, technique, originalCost);
    }

    @Redirect(
            method = "executeKiAttack",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/stats/character/Resources;setCurrentEnergy(F)V"),
            require = 0,
            remap = false
    )
    // Handles the recordTechniqueKiSpent logic for this class.
    private static void dmzrevamp$recordTechniqueKiSpent(Resources resources, float newEnergy, LivingEntity caster, Level level, KiAttackData technique, StatsData data, float chargePercent) {
        float previousEnergy = resources.getCurrentEnergy();
        resources.setCurrentEnergy(newEnergy);
        if (caster instanceof ServerPlayer player && previousEnergy > newEnergy) {
            ClassSkillEvents.onKiActionSpent(player, data, previousEnergy - newEnergy);
        }
    }
}
