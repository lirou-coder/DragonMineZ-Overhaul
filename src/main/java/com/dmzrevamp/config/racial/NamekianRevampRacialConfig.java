package com.dmzrevamp.config.racial;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NamekianRevampRacialConfig {
    public Map<String, String> _comments = createComments();
    public boolean enabled = true;
    public int assimilationChargeTicks = 25;
    public int cooldownSeconds = 0;
    public double healthRegenRatio = 0.35D;
    public double statBoostRatio = 0.15D;
    public List<String> boostedStats = new ArrayList<>(List.of("STR", "SKP", "STM", "DEF", "VIT", "PWR", "ENE"));
    public boolean allowNamekianPlayers = true;
    public boolean allowNamekianNpcs = true;
    public double effectDecayPerUse = 0.02D;
    public double maxBonusCurrentStatRatio = 1.0D;

    public static Map<String, String> createComments() {
        Map<String, String> comments = new LinkedHashMap<>();
        comments.put("enabled", "When false, Namekian Revamp Assimilation is disabled.");
        comments.put("assimilationChargeTicks", "Ticks the racial action must be charged. DMZ's original default is 25.");
        comments.put("cooldownSeconds", "Cooldown after a successful assimilation. The original DMZ skill has no cooldown.");
        comments.put("healthRegenRatio", "Max-health ratio healed after assimilation. DMZ's original default is 0.35.");
        comments.put("statBoostRatio", "Ratio of the Namekian's current raw stats gained per assimilation before use decay. DMZ's original default is 0.15.");
        comments.put("boostedStats", "Stats boosted by assimilation. Overhaul defaults to every base stat: STR, SKP, STM, DEF, VIT, PWR and ENE.");
        comments.put("allowNamekianPlayers", "Allows assimilation of other Namekian players when all normal power checks pass.");
        comments.put("allowNamekianNpcs", "Allows assimilation of DMZ Namekian NPCs and non-master Piccolo entities.");
        comments.put("effectDecayPerUse", "Each successful assimilation loses this ratio of its boost per previous assimilation. 0.02 = 2% less per use and determines the usable assimilation count.");
        comments.put("maxBonusCurrentStatRatio", "Maximum permanent Assimilation bonus ratio based only on each untransformed base stat, before decay scaling. The bonus is stored in one line and accepts multipliers.");
        return comments;
    }
}
