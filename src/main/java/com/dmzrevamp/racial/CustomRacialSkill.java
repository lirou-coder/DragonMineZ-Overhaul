// Common contract for racial skills that replace or extend Dragon Mine Z's built-in racial action system.
package com.dmzrevamp.racial;

import com.dragonminez.client.gui.utilitymenu.ButtonInfo;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.extras.ActionMode;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public interface CustomRacialSkill {
    String id();

    default boolean showsRacialActionButton(StatsData data) {
        return false;
    }

    @OnlyIn(Dist.CLIENT)
    default ButtonInfo buildButtonInfo(StatsData data) {
        boolean active = isActive(data);
        return new ButtonInfo(
                Component.translatable("gui.action.dragonminez.racial." + id()).withStyle(ChatFormatting.BOLD),
                Component.translatable("gui.action.dragonminez." + active),
                active
        );
    }

    @OnlyIn(Dist.CLIENT)
    default boolean handleButtonClick(StatsData data, boolean rightClick) {
        return false;
    }

    default boolean isActive(StatsData data) {
        return data != null && data.getStatus().getSelectedAction() == ActionMode.RACIAL;
    }

    default Integer getActionCharge(ServerPlayer player, StatsData data) {
        return null;
    }

    default Boolean performAction(ServerPlayer player, StatsData data) {
        return null;
    }

    default String cooldownKey() {
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    default Component getSkillTitle() {
        return Component.translatable("skill.dragonminez.racial_" + id());
    }

    @OnlyIn(Dist.CLIENT)
    default Component getSkillDescription(StatsData data) {
        return Component.translatable("skill.dragonminez.racial_" + id() + ".desc");
    }

    @OnlyIn(Dist.CLIENT)
    default Component getRaceSelectionDescription() {
        return Component.translatable("skill.dragonminez.racial_" + id() + ".desc");
    }
}
