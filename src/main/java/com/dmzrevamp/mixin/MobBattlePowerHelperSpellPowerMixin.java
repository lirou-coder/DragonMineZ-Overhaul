package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.battlepower.AccurateMobBattlePowerCalculator;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(targets = "com.dragonminez.common.init.entities.MobBattlePowerHelper", remap = false)
public abstract class MobBattlePowerHelperSpellPowerMixin {
    /**
     * @author Dragon Mine Z: Overhaul
     * @reason DMZ excludes its own mobs from accurate BP and consequently falls back to
     * maxHealth + attackDamage * 5. Players are the only entities that must remain managed
     * outside the mob calculator.
     */
    @Overwrite
    public static boolean isDmzManaged(LivingEntity entity) {
        return entity instanceof Player;
    }

    /**
     * @author Dragon Mine Z: Overhaul
     * @reason Replace DMZ's linear mob BP helper with the curved player-style mob BP formula.
     */
    @Overwrite
    public static int calculate(LivingEntity entity) {
        if (entity instanceof Player) {
            return 0;
        }
        long curvedBattlePower = AccurateMobBattlePowerCalculator.calculateCurvedBattlePower(entity);
        return AccurateMobBattlePowerCalculator.toStoredVisibleBattlePower(curvedBattlePower);
    }
}
