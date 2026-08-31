package com.dmzrevamp.mixin.client;

import com.dragonminez.client.gui.MasterTextScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = MasterTextScreen.class, remap = false)
public abstract class MasterTextScreenWeightLimitMixin {
    private static final int DMZREVAMP_WEIGHT_DIGITS = 9;

    @ModifyArg(
            method = {"initWeightService", "initPiccolo"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/EditBox;m_94199_(I)V"),
            index = 0,
            require = 0
    )
    private int dmzrevamp$allowHeavierMasterWeightInput(int original) {
        // The text box limits how many digits a player can type before the server receives the request.
        return DMZREVAMP_WEIGHT_DIGITS;
    }
}
