package com.dmzrevamp.mixin;

import com.dragonminez.common.init.item.WeightItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = WeightItem.class, remap = false)
public abstract class WeightItemRevampLimitMixin {
    private static final int DMZREVAMP_MAX_WEIGHT_KG = 100_000_000;

    @Inject(method = "setWeight", at = @At("HEAD"), cancellable = true)
    private static void dmzrevamp$allowHeavierWeights(ItemStack stack, int weight, CallbackInfo ci) {
        int clampedWeight = Math.max(1, Math.min(DMZREVAMP_MAX_WEIGHT_KG, weight));
        // Weight is stored in item NBT; int storage safely supports the new 100,000,000 kg limit.
        stack.getOrCreateTag().putInt("WeightValue", clampedWeight);
        ci.cancel();
    }
}
