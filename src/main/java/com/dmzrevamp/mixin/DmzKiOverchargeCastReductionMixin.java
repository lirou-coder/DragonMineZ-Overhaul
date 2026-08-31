package com.dmzrevamp.mixin;

import com.dmzrevamp.compat.DmzKiOverchargeCompat;
import com.dmzrevamp.revamp.ki.KiAttackOverhaul;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.techniques.Techniques;
import com.dragonminez.server.events.players.TickHandler;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TickHandler.class)
public abstract class DmzKiOverchargeCastReductionMixin {
    @Redirect(
            method = "handleTechniqueCharge",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/stats/techniques/Techniques;setTechniqueChargePercent(F)V"),
            remap = false,
            require = 0
    )
    private static void dmzrevamp$applySpdCastReductionWithDmzKiOvercharge(Techniques techniques, float percent, ServerPlayer player, StatsData data) {
        float currentPercent = techniques.getTechniqueChargePercent();
        float maxPercent = DmzKiOverchargeCompat.effectiveMaxChargePercent(KiAttackOverhaul.maxChargePercent());
        techniques.setTechniqueChargePercent(KiAttackOverhaul.applyOverchargeCastReduction(currentPercent, percent, data, maxPercent));
    }
}
