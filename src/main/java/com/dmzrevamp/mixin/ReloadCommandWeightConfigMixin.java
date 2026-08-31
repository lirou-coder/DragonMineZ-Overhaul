package com.dmzrevamp.mixin;

import com.dmzrevamp.config.WeightMovementPenaltyConfig;
import com.dragonminez.server.commands.ReloadCommand;
import net.minecraft.commands.CommandSourceStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Guarantees the standalone movement-weight JSON follows /dmzreload all|config. */
@Mixin(value = ReloadCommand.class, remap = false)
public abstract class ReloadCommandWeightConfigMixin {
    @Inject(method = "executeReload", at = @At("RETURN"), remap = false)
    private static void dmzrevamp$reloadWeightMovementConfig(CommandSourceStack source,
                                                              String rawScope,
                                                              CallbackInfoReturnable<Integer> cir) {
        if (cir.getReturnValueI() == 1
                && ("all".equalsIgnoreCase(rawScope) || "config".equalsIgnoreCase(rawScope))) {
            WeightMovementPenaltyConfig.reload();
        }
    }
}
