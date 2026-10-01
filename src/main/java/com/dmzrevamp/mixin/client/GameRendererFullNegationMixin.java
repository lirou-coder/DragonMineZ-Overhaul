package com.dmzrevamp.mixin.client;

import com.dmzrevamp.client.FullNegationClientState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererFullNegationMixin {
    // Same SRG/runtime constraint as LivingEntityFullNegationMixin.
    @Inject(method = "m_109117_", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzrevamp$cancelFullyNegatedDamageTilt(PoseStack poseStack, float partialTick, CallbackInfo ci) {
        if (FullNegationClientState.suppressesDamageTilt()) {
            ci.cancel();
        }
    }
}
