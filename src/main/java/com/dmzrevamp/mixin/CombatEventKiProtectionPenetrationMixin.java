package com.dmzrevamp.mixin;

import com.dragonminez.common.config.CombatConfig;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.server.events.players.combat.CombatEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CombatEvent.class)
public abstract class CombatEventKiProtectionPenetrationMixin {
    @Redirect(
            method = "lambda$overrideVanillaArmorReduction$8",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/config/CombatConfig;getKiProtectionMitigationPerLevel()D"),
            require = 0,
            remap = false
    )
    private static double dmzrevamp$applyDefensePenetrationToKiProtection(CombatConfig config,
                                                                          double rawDamage,
                                                                          double defensePenetration,
                                                                          Player victim,
                                                                          LivingDamageEvent event,
                                                                          StatsData stats) {
        return config.getKiProtectionMitigationPerLevel() * (1D - clamp01(defensePenetration));
    }

    private static double clamp01(double value) {
        if (!Double.isFinite(value)) {
            return 0D;
        }
        return Math.max(0D, Math.min(1D, value));
    }
}
