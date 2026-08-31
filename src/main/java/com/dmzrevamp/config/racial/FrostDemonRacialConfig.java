package com.dmzrevamp.config.racial;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FrostDemonRacialConfig {
    public Map<String, String> _comments = createComments();
    public double speedAndPowerBoostPerMissingHpPercent = 0.005D;
    public double attackStaminaCostReduction = 0.5D;
    public List<String> boostedStats = new ArrayList<>(List.of("SKP", "PWR"));

    private static Map<String, String> createComments() {
        Map<String, String> comments = new LinkedHashMap<>();
        comments.put("speedAndPowerBoostPerMissingHpPercent", "Multiplicative bonus per 1% missing HP. 0.005 = +0.5% per missing HP percent.");
        comments.put("attackStaminaCostReduction", "Stamina cost reduction for attacks only. 0.5 = 50% less attack stamina cost.");
        comments.put("boostedStats", "DMZ stats affected by Dangerously Fast.");
        return comments;
    }
}
