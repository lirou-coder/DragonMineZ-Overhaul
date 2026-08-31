// Client-facing Saiyan racial skill entry; the server awards the Zenkai bonuses in SaiyanRpgZenkaiEvents.
package com.dmzrevamp.racial.impl;

import com.dmzrevamp.racial.CustomRacialSkill;
import com.dmzrevamp.config.racial.DmzRevampRacialConfigs;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class SaiyanRpgZenkaiRacialSkill implements CustomRacialSkill {
    public static final String COOLDOWN_KEY = "DmzRevampSaiyanZenkai";

    @Override
    // This id must match the racialSkill value written in the Saiyan race config.
    public String id() {
        return "saiyanrevamp";
    }

    @Override
    // DMZ uses this key to show and reset the Zenkai cooldown.
    public String cooldownKey() {
        return COOLDOWN_KEY;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    // Text shown as the skill name in DMZ menus.
    public Component getSkillTitle() {
        return Component.literal("Zenkai");
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    // Text shown when the player views the Saiyan racial skill after character creation.
    public Component getSkillDescription(StatsData data) {
        return createDescription();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    // Text shown on the race-selection screen before the character exists.
    public Component getRaceSelectionDescription() {
        return createDescription();
    }

    // Builds the translated description using the configured cooldown duration.
    private Component createDescription() {
        var config = DmzRevampRacialConfigs.saiyanRpg();
        return Component.translatable(
                "skill.dragonminez.racial_saiyanrevamp.desc",
                config.cooldownSeconds
        );
    }
}
