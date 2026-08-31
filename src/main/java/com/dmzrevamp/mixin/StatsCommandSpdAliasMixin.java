package com.dmzrevamp.mixin;

import com.dragonminez.server.commands.StatsCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(StatsCommand.class)
public abstract class StatsCommandSpdAliasMixin {
    @Redirect(
            method = "modifyStats",
            at = @At(value = "INVOKE", target = "Ljava/lang/String;toUpperCase()Ljava/lang/String;"),
            remap = false
    )
    private static String dmzrevamp$mapSpdAliasToSkp(String stat) {
        String upper = stat.toUpperCase();
        return "SPD".equals(upper) ? "SKP" : upper;
    }
}
