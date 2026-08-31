package com.dmzrevamp.mixin.client;

import com.dmzrevamp.revamp.ki.KiAttackOverhaul;
import com.dragonminez.client.gui.hud.TechniqueChargeOverlay;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TechniqueChargeOverlay.class)
public abstract class TechniqueChargeOverlaySecondBarCompatMixin {
    @Shadow(remap = false)
    private static volatile float currentChargePercent;

    @Redirect(
            method = "lambda$static$0",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;m_280163_(Lnet/minecraft/resources/ResourceLocation;IIFFIIII)V", ordinal = 2),
            remap = false
    )
    private static void dmzrevamp$drawSecondOverchargeBarWithDmzKiOvercharge(GuiGraphics graphics, ResourceLocation texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
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
