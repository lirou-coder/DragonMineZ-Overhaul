package com.dmzrevamp.mixin.client;

import com.dmzrevamp.revamp.classes.DmzClassConfigManager;
import com.dragonminez.client.gui.character.PartyMenuScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PartyMenuScreen.class)
public abstract class PartyMenuScreenMixin {
    private static final ResourceLocation DMZREVAMP_SMOOTH_FONT =
            ResourceLocation.fromNamespaceAndPath("dragonminez", "smooth");

    @Redirect(
            method = "lambda$renderRightPanelDetails$15",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dragonminez/client/gui/character/PartyMenuScreen;tr(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;"
            ),
            remap = false,
            require = 0
    )
    private MutableComponent dmzrevamp$renameServerStatsSkpKey(PartyMenuScreen screen, String key, Object[] args) {
        if ("gui.dragonminez.character_stats.skp".equals(key)) {
            return screen.tr("gui.dragonminez.character_stats.spd");
        }

        String classPrefix = "class.dragonminez.";
        if (key != null && key.startsWith(classPrefix) && !key.contains(".passive")) {
            String classId = key.substring(classPrefix.length());
            int color = DmzClassConfigManager.getDisplayColor(classId);
            return Component.literal(DmzClassConfigManager.getDisplayName(classId))
                    .withStyle(Style.EMPTY.withFont(DMZREVAMP_SMOOTH_FONT).withColor(TextColor.fromRgb(color)));
        }

        // PartyMenuScreen.tr() applies the DMZ smooth font. Rebuilding the
        // component with Component.translatable() here discarded that style.
        return screen.tr(key, args);
    }
}
