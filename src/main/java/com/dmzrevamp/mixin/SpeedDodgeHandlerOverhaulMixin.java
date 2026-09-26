package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.battlepower.AccurateMobBattlePowerCalculator;
import com.dragonminez.common.config.CombatConfig;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.server.events.players.combat.SpeedDodgeHandler;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(SpeedDodgeHandler.class)
public abstract class SpeedDodgeHandlerOverhaulMixin {
    @Redirect(
            method = "dodgeChance",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/stats/StatsData;getBattlePower()F"),
            remap = false
    )
    private static float dmzrevamp$useExactPlayerBattlePower(StatsData data) {
        double exact = data.getBattlePowerExact();
        return (float) Math.min(Math.max(0D, exact), Float.MAX_VALUE);
    }

    @ModifyVariable(
            method = "dodgeChance",
            at = @At(value = "STORE"),
            ordinal = 0,
            remap = false,
            require = 0
    )
    private static double dmzrevamp$useCustomMobBattlePower(
            double original,
            StatsData data,
            LivingEntity attacker,
            int meditationLevel,
            CombatConfig config
    ) {
        double exact = AccurateMobBattlePowerCalculator.calculateCurvedBattlePowerExact(attacker);
        return Double.isFinite(exact) && exact > 0D ? exact : original;
    }
}
