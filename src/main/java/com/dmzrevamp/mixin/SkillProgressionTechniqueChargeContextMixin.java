package com.dmzrevamp.mixin;

import com.dmzrevamp.compat.DmzSkillProgressionCompat;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.server.events.players.TickHandler;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TickHandler.class, priority = 900)
public abstract class SkillProgressionTechniqueChargeContextMixin {
    @Inject(method = "handleTechniqueCharge", at = @At("HEAD"), remap = false)
    private static void dmzrevamp$beginSkillProgressionChargeContext(ServerPlayer player, StatsData data, CallbackInfo ci) {
        DmzSkillProgressionCompat.beginTechniqueCharge(player, data);
    }

    @Inject(method = "handleTechniqueCharge", at = @At("RETURN"), remap = false)
    private static void dmzrevamp$endSkillProgressionChargeContext(ServerPlayer player, StatsData data, CallbackInfo ci) {
        DmzSkillProgressionCompat.endTechniqueCharge();
    }
}
