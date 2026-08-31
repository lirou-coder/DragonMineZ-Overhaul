package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.classes.skills.ClassSkillEvents;
import com.dragonminez.common.network.C2S.KiBlastC2S;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(KiBlastC2S.class)
public abstract class KiBlastC2SClassSkillMixin {
    @ModifyVariable(
            method = "lambda$handle$0",
            at = @At(value = "STORE"),
            ordinal = 0,
            require = 0,
            remap = false
    )
    // Handles the adjustClassSkillKiBlastCost logic for this class.
    private static int dmzrevamp$adjustClassSkillKiBlastCost(int originalCost, ServerPlayer player, KiBlastC2S packet, StatsData data) {
        return ClassSkillEvents.adjustKiBlastCost(player, data, originalCost);
    }
}
