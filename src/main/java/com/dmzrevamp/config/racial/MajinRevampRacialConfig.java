package com.dmzrevamp.config.racial;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MajinRevampRacialConfig {
    public Map<String, String> _comments = createComments();
    public boolean enabled = true;
    public boolean allowMobAbsorption = true;
    public boolean allowPlayerAbsorption = true;
    public int absorptionChargeTicks = 25;
    public double healthRegenRatio = 0.25D;
    public double statCopyRatio = 0.05D;
    public double effectDecayPerUse = 0.02D;
    public double maxBonusBaseStatRatio = 1.0D;
    public double targetCurrentHealthDamageThreshold = 1.0D;
    public List<String> boostedStats = new ArrayList<>(List.of("STR", "SKP", "PWR", "DEF", "STM"));

    public static Map<String, String> createComments() {
        Map<String, String> comments = new LinkedHashMap<>();
        comments.put("enabled", "When false, the custom Majin Absorption racial action does not work.");
        comments.put("allowMobAbsorption", "When true, Majin Absorption can absorb non-player mobs.");
        comments.put("allowPlayerAbsorption", "When true, Majin Absorption can absorb player targets.");
        comments.put("absorptionChargeTicks", "Ticks the racial action must be charged before Majin Absorption occurs. DMZ's default Majin absorption charge is 25 ticks.");
        comments.put("healthRegenRatio", "Max health ratio healed after successful absorption. 0.25 = 25% max health.");
        comments.put("statCopyRatio", "Ratio copied from target stats. 0.05 = 5% of target stat values.");
        comments.put("effectDecayPerUse", "Each successful absorption loses this ratio of its boost per previous absorption. 0.02 = 2% less per use.");
        comments.put("maxBonusBaseStatRatio", "Maximum permanent absorption bonus per stat compared to the absorber's base stat before decay scaling. 1.0 with effectDecayPerUse 0.02 allows up to 50x base stat total; 0 decay removes this cap.");
        comments.put("targetCurrentHealthDamageThreshold", "Absorption can occur when target current HP multiplied by this value is lower than the Majin player's Melee or Ki Damage. 1.0 = current HP must be lower than damage.");
        comments.put("boostedStats", "DMZ stats affected by Majin Absorption permanent bonuses. DEF and STM use the target's RES/base defense stat value.");
        return comments;
    }
}
