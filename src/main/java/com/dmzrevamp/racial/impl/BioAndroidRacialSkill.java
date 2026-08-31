// Client-facing Bio Android racial skill entry; the actual mixed racial effects are handled by the shared racial events.
package com.dmzrevamp.racial.impl;

import com.dmzrevamp.racial.CustomRacialSkill;
import com.dmzrevamp.config.racial.DmzRevampRacialConfigs;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class BioAndroidRacialSkill implements CustomRacialSkill {
    @Override
    // This id must match the racialSkill value written in the Bio Android race config.
    public String id() {
        return "bioandroidrevamp";
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    // Text shown as the skill name in DMZ menus.
    public Component getSkillTitle() {
        return Component.literal("the Perfect DNA");
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    // Text shown when the player views the Bio Android racial skill after character creation.
    public Component getSkillDescription(StatsData data) {
        return createDescription();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    // Text shown on the race-selection screen before the character exists.
    public Component getRaceSelectionDescription() {
        return createDescription();
    }

    // Builds the translated description using the configured percentage of borrowed racial power.
    private Component createDescription() {
        var config = DmzRevampRacialConfigs.bioAndroid();
        return Component.translatable(
                "skill.dragonminez.racial_bioandroidrevamp.desc",
                config.effectMultiplier
        );
    }
}
