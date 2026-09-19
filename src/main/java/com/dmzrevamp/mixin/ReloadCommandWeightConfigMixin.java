package com.dmzrevamp.mixin;

import com.dmzrevamp.config.WeightMovementPenaltyConfig;
import com.dmzrevamp.revamp.battlepower.BattlePowerReloadService;
import com.dragonminez.server.commands.ReloadCommand;
import net.minecraft.commands.CommandSourceStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Guarantees the standalone movement-weight JSON follows /dmzreload all|config. */
@Mixin(value = ReloadCommand.class, remap = false)
public abstract class ReloadCommandWeightConfigMixin {
    @Inject(method = "executeReload", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dmzrevamp$acceptConfigsAlias(CommandSourceStack source,
                                                      String rawScope,
                                                      CallbackInfoReturnable<Integer> cir) {
        if ("configs".equalsIgnoreCase(rawScope)) {
            cir.setReturnValue(ReloadCommand.executeReload(source, "config"));
        }
    }

    @Inject(method = "executeReload", at = @At("RETURN"), remap = false)
    private static void dmzrevamp$reloadWeightMovementConfig(CommandSourceStack source,
                                                              String rawScope,
                                                              CallbackInfoReturnable<Integer> cir) {
        if (cir.getReturnValueI() == 1
                && ("all".equalsIgnoreCase(rawScope) || "config".equalsIgnoreCase(rawScope)
                || "configs".equalsIgnoreCase(rawScope))) {
            WeightMovementPenaltyConfig.reload();
        }
        if (cir.getReturnValueI() == 1
                && ("all".equalsIgnoreCase(rawScope) || "config".equalsIgnoreCase(rawScope))) {
            BattlePowerReloadService.recalculateAll(source.getServer());
        }
    }
}
