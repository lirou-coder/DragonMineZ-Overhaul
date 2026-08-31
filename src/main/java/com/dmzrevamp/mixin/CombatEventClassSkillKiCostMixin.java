package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.classes.skills.ClassSkillEvents;
import com.dragonminez.common.stats.character.Resources;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.server.events.players.combat.CombatEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CombatEvent.class)
public abstract class CombatEventClassSkillKiCostMixin {
    @Redirect(
            method = "lambda$onLivingHurt$3",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/stats/character/Resources;removeEnergy(F)V"),
            require = 0,
            remap = false
    )
    // Handles the refundKiSupportCombatKiCost logic for this class.
    private static void dmzrevamp$refundKiSupportCombatKiCost(Resources resources,
                                                              float originalCost,
                                                              LivingHurtEvent event,
                                                              boolean[] handled,
                                                              Player attacker,
                                                              boolean punchMachine,
                                                              double[] damage,
                                                              LivingEntity target,
                                                              double[] originalDamage,
                                                              StatsData data) {
        int adjustedCost = Math.round(originalCost);
        if (attacker instanceof ServerPlayer serverPlayer) {
            adjustedCost = ClassSkillEvents.adjustNonDodgeKiCost(serverPlayer, data, adjustedCost);
        }
        resources.removeEnergy(adjustedCost);
    }
}
