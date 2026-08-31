// Default tuning values for Human's Ki Boosting Body and Android-upgraded body behavior.
package com.dmzrevamp.config.racial;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HumanRpgRacialConfig {
    public Map<String, String> _comments = createComments();
    public double kiRegenBonus = 0.25D;
    public double androidUpgradedKiRegenBonusMultiplier = 2.0D;
    public double fullKiPowerBoost = 0.20D;
    public double androidUpgradedFullKiPowerBoostMultiplier = 0.5D;
    public double fullKiThreshold = 0.85D;
    public List<String> boostedStats = new ArrayList<>(List.of("STR", "SKP", "PWR", "DEF"));

    // Adds human-readable notes to the generated JSON without affecting gameplay values.
    public static Map<String, String> createComments() {
        Map<String, String> comments = new LinkedHashMap<>();
        comments.put("kiRegenBonus", "Extra Ki regeneration ratio. 0.25 = +25% regen, so 10 Ki/s becomes 12.5 Ki/s.");
        comments.put("androidUpgradedKiRegenBonusMultiplier", "Multiplier applied to kiRegenBonus when Android Upgraded is active. 2.0 makes the default +25% become +50%.");
        comments.put("fullKiPowerBoost", "Multiplicative stat boost while at or above the full Ki threshold. 0.20 = +20%.");
        comments.put("androidUpgradedFullKiPowerBoostMultiplier", "Multiplier applied to fullKiPowerBoost when Android Upgraded is active. 0.5 makes the default +20% become +10%.");
        comments.put("fullKiThreshold", "Current Ki ratio required for the stat boost. 0.85 = 85% Ki or higher.");
        comments.put("boostedStats", "DMZ stats boosted by Ki Boosting Body while the threshold is met. RES is interpreted as DEF only for this passive.");
        return comments;
    }
}
