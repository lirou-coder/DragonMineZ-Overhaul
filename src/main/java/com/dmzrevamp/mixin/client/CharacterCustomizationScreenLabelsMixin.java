package com.dmzrevamp.mixin.client;

import com.dmzrevamp.revamp.classes.DmzClassConfigManager;
import com.dmzrevamp.revamp.classes.skills.ClassSkillHelper;
import com.dragonminez.client.gui.character.CharacterCustomizationScreen;
import com.dragonminez.common.stats.character.Character;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CharacterCustomizationScreen.class)
public abstract class CharacterCustomizationScreenLabelsMixin {
    private static final ResourceLocation DMZ_FONT = ResourceLocation.fromNamespaceAndPath("dragonminez", "smooth");

    @Shadow(remap = false)
    private Character character;

    @ModifyConstant(method = "renderBaseStatsInline", constant = @Constant(stringValue = "SKP"), remap = false, require = 0)
    // Handles the renameSkpLabel logic for this class.
    private String dmzrevamp$renameSkpLabel(String original) {
        return Component.translatable("gui.dmzrevamp.character_stats.spd").getString();
    }

    @Redirect(
            method = "renderBaseStatsInline",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dragonminez/client/gui/character/CharacterCustomizationScreen;tr(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;"
            ),
            remap = false,
            require = 0
    )
    // Replaces the class selection SKP label translation with the SPD label.
    private MutableComponent dmzrevamp$redirectClassSelectionSpdLabel(CharacterCustomizationScreen instance, String key, Object[] args) {
        if ("gui.dragonminez.character_stats.skp".equals(key)) {
            return Component.translatable("gui.dmzrevamp.character_stats.spd").withStyle(Style.EMPTY.withFont(DMZ_FONT));
        }
        if ("gui.dragonminez.character_stats.skp.desc".equals(key)) {
            return Component.translatable("gui.dragonminez.character_stats.spd.desc").withStyle(Style.EMPTY.withFont(DMZ_FONT));
        }
        return instance.tr(key, args);
    }

    @Redirect(
            method = "renderBaseStatsInline",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/chat/Component;m_237115_(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;"
            ),
            remap = false,
            require = 0
    )
    // Replaces the class selection SKP hover title and description with the SPD text.
    private MutableComponent dmzrevamp$redirectClassSelectionSpdTooltip(String key) {
        if ("gui.dragonminez.character_stats.skp".equals(key)) {
            return Component.translatable("gui.dmzrevamp.character_stats.spd")
                    .withStyle(Style.EMPTY.withFont(DMZ_FONT));
        }
        if ("gui.dragonminez.character_stats.skp.desc".equals(key)) {
            return Component.translatable("gui.dragonminez.character_stats.spd.desc")
                    .withStyle(Style.EMPTY.withFont(DMZ_FONT).withColor(ChatFormatting.GRAY));
        }
        return Component.translatable(key).withStyle(Style.EMPTY.withFont(DMZ_FONT));
    }

    @Redirect(
            method = "renderPassiveDescription",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dragonminez/client/gui/character/CharacterCustomizationScreen;tr(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;"
            ),
            remap = false,
            require = 0
    )
    private MutableComponent dmzrevamp$replaceClassPassiveDescription(CharacterCustomizationScreen instance, String key, Object[] args) {
        if (key != null && key.startsWith("class.dragonminez.") && key.endsWith(".passive.desc") && character != null) {
            String skillId = ClassSkillHelper.getSkillForClass(character.getCharacterClass());
            if (skillId != null) {
                String description = ClassSkillHelper.getDescription(null, skillId);
                if (description != null && !description.isEmpty()) {
                    return Component.literal(description).withStyle(Style.EMPTY.withFont(DMZ_FONT));
                }
            }
        }
        return instance.tr(key, args);
    }

    @ModifyArg(
            method = "renderAuraClassText",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/client/util/TextUtil;drawCenteredStringWithBorder(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V", ordinal = 1),
            index = 2,
            remap = false
    )
    // Replaces the rendered class name with the class config display name.
    private Component dmzrevamp$replaceClassDisplayName(Component original) {
        String classId = character != null ? character.getCharacterClass() : "";
        int color = DmzClassConfigManager.getDisplayColor(classId);
        return Component.literal(DmzClassConfigManager.getDisplayName(classId))
                .withStyle(style -> style.withFont(DMZ_FONT).withColor(TextColor.fromRgb(color & 0xFFFFFF)));
    }

    @ModifyArg(
            method = "renderAuraClassText",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/client/util/TextUtil;drawCenteredStringWithBorder(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V", ordinal = 1),
            index = 5,
            remap = false
    )
    // Replaces the rendered class name color with the class config color.
    private int dmzrevamp$replaceClassDisplayColor(int original) {
        String classId = character != null ? character.getCharacterClass() : "";
        return DmzClassConfigManager.getDisplayColor(classId);
    }
}
