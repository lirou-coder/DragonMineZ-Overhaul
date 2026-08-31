package com.dmzrevamp.mixin;

import com.dmzrevamp.config.DmzRevampConfig;
import com.dragonminez.common.network.C2S.NPCActionC2S;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = NPCActionC2S.class, remap = false)
public abstract class NPCActionWeightLimitMixin {
    @ModifyConstant(method = "giveWeight", constant = @Constant(intValue = 100000), require = 0)
    private static int dmzrevamp$allowHeavierMasterWeights(int original) {
        // Master NPCs clamp the requested weight before creating the item, so the new limit must live here too.
        return DmzRevampConfig.MAX_MASTER_WEIGHT.get();
    }
}
