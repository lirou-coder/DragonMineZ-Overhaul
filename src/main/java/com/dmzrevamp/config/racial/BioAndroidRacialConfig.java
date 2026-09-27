// Default tuning values for Bio Android's weaker copies of other racial passives.
package com.dmzrevamp.config.racial;

import com.google.gson.annotations.SerializedName;

import java.util.LinkedHashMap;
import java.util.Map;

public class BioAndroidRacialConfig {
    public Map<String, String> _comments = createComments();
    @SerializedName(value = "effectMultiplier", alternate = {"effectReduction"})
    public double effectMultiplier = 0.15D;
    public double healthRegenMultiplier = 0.5D;

    // Adds human-readable notes to the generated JSON without affecting gameplay values.
    static Map<String, String> createComments() {
        Map<String, String> comments = new LinkedHashMap<>();
        comments.put("effectMultiplier", "Multiplier applied to borrowed Human, Saiyan, Frost Demon and Namekian effects. 0.15 = 15% strength. Native Bio Android actions are never scaled.");
        comments.put("healthRegenMultiplier", "Multiplier applied to Bio-Android race HP regeneration. 0.5 matches the standard Overhaul race HP regeneration rate.");
        return comments;
    }
}
