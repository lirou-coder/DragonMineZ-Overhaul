package com.dmzrevamp.racial.impl;

import com.dmzrevamp.config.racial.DmzRevampRacialConfigs;
import com.dmzrevamp.racial.CustomRacialSkill;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class FrostDemonRevampRacialSkill implements CustomRacialSkill {
    @Override
    public String id() {
        return "frostrevamp";
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public Component getSkillTitle() {
        return Component.literal("Dangerously Fast");
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public Component getSkillDescription(StatsData data) {
        return createDescription();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public Component getRaceSelectionDescription() {
        return createDescription();
    }

    private Component createDescription() {
        int staminaPercent = (int) Math.round(DmzRevampRacialConfigs.frostDemon().attackStaminaCostReduction * 100D);
        return Component.translatable("skill.dragonminez.racial_frostrevamp.desc", staminaPercent);
    }
}
