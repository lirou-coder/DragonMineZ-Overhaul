package com.dmzrevamp.revamp.strike;

import java.util.Locale;

public enum CustomStrikeType {
    BASIC("basic", 1.6F, 1.0F, "combo1", 40, 1.0F),
    AIR("air", 1.4F, 1.0F, "combo2", 40, 1.0F),
    CHARGE("charge", 2.0F, 1.0F, "combo3", 25, 1.0F),
    METEOR_COMBINATION("meteor_combination", 2.0F, 1.0F, "skp.kaioken_attack", 50, 1.0F),
    FAST_PUNCH("fast_punch", 1.4F, 1.0F, "combo6", 25, 1.0F),
    STRONG_PUNCH("strong_punch", 2.0F, 1.0F, "combo7", 20, 1.0F),
    EVASIVE("evasive", 0.2F, 1.0F, "technique.evasive", 20, 0.0F);

    private final String translationSuffix;
    private final float defaultDamageMultiplier;
    private final float cooldownMultiplier;
    private final String animationId;
    private final int durationTicks;
    private final float defaultSpeedMultiplier;

    CustomStrikeType(String translationSuffix, float defaultDamageMultiplier, float cooldownMultiplier, String animationId, int durationTicks, float defaultSpeedMultiplier) {
        this.translationSuffix = translationSuffix;
        this.defaultDamageMultiplier = defaultDamageMultiplier;
        this.cooldownMultiplier = cooldownMultiplier;
        this.animationId = animationId;
        this.durationTicks = durationTicks;
        this.defaultSpeedMultiplier = defaultSpeedMultiplier;
    }

    public String translationSuffix() {
        return translationSuffix;
    }

    public float defaultDamageMultiplier() {
        return defaultDamageMultiplier;
    }

    public float minDamageMultiplier() {
        return defaultDamageMultiplier * 0.5F;
    }

    public float maxDamageMultiplier() {
        return defaultDamageMultiplier * 2.0F;
    }

    public float cooldownMultiplier() {
        return cooldownMultiplier;
    }

    public String animationId() {
        return animationId;
    }

    public int durationTicks() {
        return durationTicks;
    }

    public float defaultSpeedMultiplier() {
        return defaultSpeedMultiplier;
    }

    public boolean isEvasive() {
        return this == EVASIVE;
    }

    public static CustomStrikeType parse(String value) {
        if (value == null || value.isBlank()) {
            return BASIC;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return BASIC;
        }
    }
}
