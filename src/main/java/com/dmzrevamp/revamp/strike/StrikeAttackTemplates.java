package com.dmzrevamp.revamp.strike;

import com.dragonminez.common.stats.techniques.PredefinedTechniques;
import com.dragonminez.common.stats.techniques.StrikeAttackData;

public final class StrikeAttackTemplates {
    public static final String ANDROID_ABSORPTION = "android_absorption";
    public static final String SLEEP_RECOVERY = "sleep_recovery";
    public static final String NAMEKIAN_REGENERATION = "namekian_regeneration";

    private StrikeAttackTemplates() {
    }

    public static void registerRaceExclusiveDefaults() {
        StrikeAttackData absorption = register(ANDROID_ABSORPTION, "technique.dmzrevamp.android_absorption", "System", 1.25F, "cell_absorb", 40);
        absorption.setCooldown(1200);
        StrikeAttackData sleep = register(SLEEP_RECOVERY, "technique.dmzrevamp.sleep_recovery", "System", 1.0F, "base.meditation", 100);
        sleep.setCooldown(1800);
        StrikeAttackData regeneration = register(NAMEKIAN_REGENERATION, "technique.dmzrevamp.namekian_regeneration", "System", 1.0F, "animation.technique.regeneration", 28);
        regeneration.setCooldown(1200);
    }

    public static StrikeAttackData copy(String id) {
        StrikeAttackData source = PredefinedTechniques.STRIKE_REGISTRY.get(id);
        if (source == null) {
            return null;
        }
        StrikeAttackData copy = new StrikeAttackData();
        copy.load(source.save());
        return copy;
    }

    private static StrikeAttackData register(String id, String name, String author, float damageMultiplier, String animationId, int durationTicks) {
        StrikeAttackData data = new StrikeAttackData();
        data.setId(id);
        data.setName(name);
        data.setAuthor(author);
        data.setDamageMultiplier(damageMultiplier);
        data.setAnimationId(animationId);
        data.setDurationTicks(durationTicks);
        data.applyConfigDefaults();
        if (data instanceof RevampStrikeAttackData revamp) {
            revamp.dmzrevamp$setCustomStrike(true);
        }
        PredefinedTechniques.STRIKE_REGISTRY.put(id, data);
        return data;
    }
}
