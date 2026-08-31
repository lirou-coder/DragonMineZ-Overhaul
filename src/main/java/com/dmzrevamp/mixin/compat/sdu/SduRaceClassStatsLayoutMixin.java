package com.dmzrevamp.mixin.compat.sdu;

import net.shurui.dev.sdu.client.gui.race.RaceClassStatsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Gives SDU's class editor enough room for Overhaul class metadata fields. */
@Pseudo
@Mixin(value = RaceClassStatsScreen.class, remap = false)
public abstract class SduRaceClassStatsLayoutMixin {
    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 260), require = 0)
    private static int dmzrevamp$extendClassEditorHeight(int original) {
        return 320;
    }

    @ModifyConstant(method = "m_7856_", constant = @Constant(intValue = 238), require = 0)
    private int dmzrevamp$moveBackButtonDown(int original) {
        return 298;
    }
}
