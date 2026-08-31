package com.dmzrevamp.mixin.compat.sdu;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/** Keeps SDU's race editor aligned with Overhaul's separated class format. */
@Pseudo
@Mixin(targets = "net.shurui.dev.sdu.client.gui.race.RaceEditScreen", remap = false)
public abstract class SduRaceEditScreenMixin {
    @Redirect(
            method = "buildSection",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/shurui/dev/sdu/client/DmzAssets;raceClasses()Ljava/util/List;"
            ),
            require = 0
    )
    private List<String> dmzrevamp$showOnlyRaceStatsEntry() {
        return List.of("race");
    }
}
