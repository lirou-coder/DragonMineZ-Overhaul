package com.dmzrevamp.mixin.client;

import com.dmzrevamp.revamp.ki.KiAttackOverhaul;
import com.dragonminez.client.gui.hud.TechniqueChargeOverlay;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TechniqueChargeOverlay.class)
public abstract class TechniqueChargeOverlayOverchargeMixin {
    @Shadow(remap = false)
    private static volatile float currentChargePercent;

    @ModifyConstant(method = "lambda$static$0", constant = @Constant(floatValue = 200.0F), remap = false)
    // Extends DMZ's animated charge HUD value so it can display up to 400 percent.
    private static float dmzrevamp$extendHudChargeClamp(float original) {
        return KiAttackOverhaul.maxChargePercent();
    }

    @Redirect(
            method = "lambda$static$0",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;m_280163_(Lnet/minecraft/resources/ResourceLocation;IIFFIIII)V", ordinal = 2),
            remap = false
    )
    // Draws the second overcharge bar inside DMZ's transformed HUD, using the same texture pass.
    private static void dmzrevamp$drawSecondOverchargeBar(GuiGraphics graphics, ResourceLocation texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        graphics.blit(texture, x, y, u, v, width, height, textureWidth, textureHeight);

        int secondWidth = Math.round(148.0F * KiAttackOverhaul.secondOverchargeFill(currentChargePercent));
        if (secondWidth <= 0) {
            return;
        }

        RenderSystem.setShaderColor(0.55F, 0.0F, 0.0F, 1.0F);
        graphics.blit(texture, 0, 0, 0.0F, 14.0F, secondWidth, 14, 256, 256);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
