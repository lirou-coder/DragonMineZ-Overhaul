package com.dmzrevamp.mixin;

import com.dmzrevamp.racial.impl.FrostDemonRevampEvents;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Resources;
import com.dragonminez.server.events.players.combat.CombatEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CombatEvent.class)
public abstract class CombatEventFrostDemonAttackStaminaMixin {
    @Redirect(
            method = "lambda$onLivingHurt$3",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/stats/character/Resources;removeStamina(F)V", ordinal = 0),
            require = 0,
            remap = false
    )
    private static void dmzrevamp$reduceFrostDemonAttackStaminaCost(Resources resources, float originalCost,
                                                                    LivingHurtEvent event, boolean[] handled,
                                                                    Player attacker, boolean punchMachine,
                                                                    double[] damage, LivingEntity target,
                                                                    double[] originalDamage, StatsData data) {
        float adjustedCost = FrostDemonRevampEvents.adjustAttackStaminaCost(data, originalCost);
        resources.removeStamina(adjustedCost);
    }
}
