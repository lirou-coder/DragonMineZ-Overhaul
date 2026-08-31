// Default tuning values for Saiyan Zenkai stat growth after surviving near-death damage.
package com.dmzrevamp.config.racial;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SaiyanRpgRacialConfig {
    public Map<String, String> _comments = createComments();
    public double activationHealthThreshold = 0.25D;
    public int maxStacks = 24;
    public int deathPreventionMaxStacks = 48;
    public double statBonusPerStack = 0.005D;
    public int cooldownSeconds = 900;
    public double zenkaiDecayPerUse = 0.02D;
    public double friendlyFistStackMultiplier = 0.5D;
    public boolean shadowDummyGiveZenkai = false;
    public boolean shadowDummyFriendlyFist = true;
    public double maxBonusBaseStatRatio = 1.0D;
    public List<String> boostedStats = new ArrayList<>(List.of("STR", "SKP", "RES", "VIT", "PWR", "ENE"));

    // Adds human-readable notes to the generated JSON without affecting gameplay values.
    public static Map<String, String> createComments() {
        Map<String, String> comments = new LinkedHashMap<>();
        comments.put("activationHealthThreshold", "HP ratio where Zenkai tracking starts. 0.25 = 25% HP or lower.");
        comments.put("maxStacks", "Maximum stacks gained from normal near-death damage.");
        comments.put("deathPreventionMaxStacks", "Maximum stacks when lethal damage is survived by death prevention.");
        comments.put("statBonusPerStack", "Flat stat gain ratio per stack. 0.005 = +0.5% of the current raw stat per stack.");
        comments.put("cooldownSeconds", "Cooldown after a Zenkai is awarded.");
        comments.put("zenkaiDecayPerUse", "Each successful Zenkai loses this ratio of its boost per previous Zenkai. 0.02 = 2% less per use.");
        comments.put("friendlyFistStackMultiplier", "Stack multiplier when damage came from a player with Friendly Fist active.");
        comments.put("shadowDummyGiveZenkai", "Whether damage caused by a DragonMineZ Shadow Dummy can contribute to Zenkai.");
        comments.put("shadowDummyFriendlyFist", "When Shadow Dummy Zenkai is enabled, treats its damage like Friendly Fist damage and applies friendlyFistStackMultiplier.");
        comments.put("maxBonusBaseStatRatio", "Maximum permanent Zenkai bonus per stat compared to the player's base stat before decay scaling. 1.0 with zenkaiDecayPerUse 0.02 allows up to 50x base stat total; 0 decay removes this cap.");
        comments.put("boostedStats", "DMZ stats affected by Zenkai permanent bonuses. RES is split into equal DEF and STM bonuses.");
        return comments;
    }
}
