// Client-facing Human racial skill entry; the server-side stat effects are applied by regen and combat events.
package com.dmzrevamp.racial.impl;

import com.dmzrevamp.config.racial.DmzRevampRacialConfigs;
import com.dmzrevamp.racial.CustomRacialSkill;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class HumanRpgAdrenalineRacialSkill implements CustomRacialSkill {
    @Override
    // This id must match the racialSkill value written in the Human race config.
    public String id() {
        return "humanrevamp";
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    // Text shown as the skill name in DMZ menus.
    public Component getSkillTitle() {
        return Component.literal("Ki Boosting Body");
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    // Text shown when the player views the Human racial skill after character creation.
    public Component getSkillDescription(StatsData data) {
        return createDescription();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    // Text shown on the race-selection screen before the character exists.
    public Component getRaceSelectionDescription() {
        return createDescription();
    }

    // Builds the translated description using the current server config percentages.
    private Component createDescription() {
        var config = DmzRevampRacialConfigs.humanRpg();
        int regenPercent = (int) Math.round(config.kiRegenBonus * 100D);
        int statPercent = (int) Math.round(config.fullKiPowerBoost * 100D);
        return Component.translatable(
                "skill.dragonminez.racial_humanrevamp.desc",
                regenPercent,
                statPercent
        );
    }
}
