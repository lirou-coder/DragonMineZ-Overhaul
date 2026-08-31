package com.dmzrevamp.mixin;

import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Resources;
import com.dragonminez.server.events.players.combat.CombatEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CombatEvent.class)
public abstract class CombatEventParryNoStaminaDrainMixin {
    @Redirect(
            method = "lambda$onLivingHurt$4",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/stats/character/Resources;removeStamina(F)V", ordinal = 1),
            require = 0,
            remap = false
    )
    private static void dmzrevamp$skipParryPostMitigationStaminaDrain(Resources resources, float amount,
                                                                      double finalHealingReduction, Player victim,
                                                                      DamageSource source, double[] currentDamage,
                                                                      double finalDefensePenetration, boolean[] wasBlocked,
                                                                      boolean[] wasParry, StatsData victimData) {
        if (!wasParry[0]) {
            resources.removeStamina(amount);
        }
    }
}
