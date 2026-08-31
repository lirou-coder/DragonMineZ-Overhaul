// Small lookup helper that connects a player's configured race to Overhaul's custom racial skill registry.
package com.dmzrevamp.racial;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.stats.StatsData;

public final class CustomRacialActionHelper {
    // This helper has only static lookups, so creating instances would serve no purpose.
    private CustomRacialActionHelper() {
    }

    // Reads the racial skill id from the loaded DMZ race config for the player's current race.
    public static String getConfiguredRacialSkillId(StatsData data) {
        String race = data.getCharacter().getRaceName();
        RaceCharacterConfig raceConfig = ConfigManager.getRaceCharacter(race);
        return raceConfig == null ? "" : raceConfig.getRacialSkill();
    }

    // Converts the configured id into the actual custom skill object, or null when the race still uses normal DMZ behavior.
    public static CustomRacialSkill getCustomRacialSkill(StatsData data) {
        return CustomRacialSkillRegistry.get(getConfiguredRacialSkillId(data));
    }
}
