package com.dmzrevamp.mixin.client;

import com.dmzrevamp.client.ClientStrikeClashState;
import com.dragonminez.client.gui.hud.BeamClashOverlay;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = BeamClashOverlay.class, remap = false)
public abstract class BeamClashOverlayStrikeTitleMixin {
    /*
     * Modify the first Component local (the title) instead of redirecting
     * Component.translatable.  The latter needs a generated refmap to locate
     * Minecraft's obfuscated invocation in a production installation, while
     * this local STORE is part of DMZ's own stable lambda bytecode.
     */
    @ModifyVariable(
            method = "lambda$static$0",
            at = @At(value = "STORE"),
            ordinal = 0,
            remap = false,
            require = 1
    )
    private static Component dmzrevamp$strikeClashTitle(Component originalTitle) {
        return ClientStrikeClashState.isActive()
                ? Component.translatable("hud.dmzrevamp.strike_clash_title")
                        .withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                : originalTitle;
    }
}
