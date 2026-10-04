package com.dmzrevamp.mixin.client;

import com.dmzrevamp.racial.CustomRacialSkill;
import com.dmzrevamp.racial.CustomRacialSkillRegistry;
import com.dragonminez.client.gui.character.RaceSelectionScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RaceSelectionScreen.class)
public abstract class RaceSelectionScreenMixin {
    @Unique
    private static final ResourceLocation DMZREVAMP$SMOOTH_FONT =
            ResourceLocation.fromNamespaceAndPath("dragonminez", "smooth");

    @Redirect(
            method = "renderRacialInfo",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dragonminez/client/gui/character/RaceSelectionScreen;tr(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;"
            ),
            remap = false,
            require = 0
    )
    // Handles the formatCustomRacialDescription logic for this class.
    private MutableComponent dmzrevamp$formatCustomRacialDescription(RaceSelectionScreen instance, String key, Object[] args) {
        if ("gui.dragonminez.race_selection.racial_skills".equals(key)) {
            return Component.translatable(key, args)
                    .withStyle(style -> style.withFont(DMZREVAMP$SMOOTH_FONT));
        }
        CustomRacialSkill customSkill = getCustomRacialSkillFromKey(key);
        if (customSkill != null) {
            if (key != null && key.endsWith(".desc")) {
                return customSkill.getRaceSelectionDescription().copy();
            }
            return customSkill.getSkillTitle().copy()
                    .withStyle(style -> style.withFont(DMZREVAMP$SMOOTH_FONT));
        }
        return Component.translatable(key, args);
    }

    // Returns the value used by getCustomRacialSkillFromKey.
    private static CustomRacialSkill getCustomRacialSkillFromKey(String key) {
        String prefix = "skill.dragonminez.racial_";
        if (key == null || !key.startsWith(prefix)) {
            return null;
        }

        CustomRacialSkillRegistry.bootstrap();
        String skillId = key.substring(prefix.length());
        if (skillId.endsWith(".desc")) {
            skillId = skillId.substring(0, skillId.length() - ".desc".length());
        }
        return CustomRacialSkillRegistry.get(skillId);
    }
}
