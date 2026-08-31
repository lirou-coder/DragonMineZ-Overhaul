package com.dmzrevamp.mixin.compat.sdu;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/** Seeds newly-created SDU races with the Overhaul race-only stats entry. */
@Pseudo
@Mixin(targets = "net.shurui.dev.sdu.client.gui.race.RaceListScreen", remap = false)
public abstract class SduRaceListScreenMixin {
    @Redirect(
            method = "seedClasses",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/shurui/dev/sdu/client/DmzAssets;raceClasses()Ljava/util/List;"
            ),
            require = 0
    )
    private static List<String> dmzrevamp$seedOnlyRaceStats() {
        return List.of("race");
    }
}
