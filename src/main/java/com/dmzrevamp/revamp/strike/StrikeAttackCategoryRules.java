package com.dmzrevamp.revamp.strike;

import com.dmzrevamp.revamp.ki.KiAttackCategory;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.techniques.StrikeAttackData;

public final class StrikeAttackCategoryRules {
    private StrikeAttackCategoryRules() {
    }

    public static KiAttackCategory classify(StrikeAttackData data) {
        return classifyPreview(data.getDamageMultiplier(), activeEffectCount(data));
    }

    public static KiAttackCategory classifyPreview(float damageMultiplier, int effectCount) {
        int tierUps = 0;
        if (effectCount > 0 && damageMultiplier > 2.0F) {
            tierUps++;
        }
        if (effectCount > 1) {
            tierUps++;
        }
        if (effectCount >= 3) {
            tierUps++;
        }
        if (tierUps >= 2) {
            return KiAttackCategory.ULTIMATE;
        }
        if (tierUps == 1) {
            return KiAttackCategory.ADVANCED;
        }
        return KiAttackCategory.BASIC;
    }

    public static int activeEffectCount(StrikeAttackData data) {
        int count = 0;
        if (data instanceof RevampStrikeAttackData revamp) {
            if (revamp.dmzrevamp$getSecondaryEffectType() != KiAttackData.SecondaryEffectType.NONE) {
                count++;
            }
            if (revamp.dmzrevamp$getThirdEffectType() != KiAttackData.SecondaryEffectType.NONE) {
                count++;
            }
            if (revamp.dmzrevamp$getFourthEffectType() != KiAttackData.SecondaryEffectType.NONE) {
                count++;
            }
            if (revamp.dmzrevamp$getExtraEffectOne().isActive()) {
                count++;
            }
            if (revamp.dmzrevamp$getExtraEffectTwo().isActive()) {
                count++;
            }
        }
        return count;
    }

    public static float secondaryWeight(float intensity, int duration) {
        // Strike secondary effects use the same cost curve as Ki secondary effects.
        return (Math.max(0.0F, intensity) / 50.0F) * Math.max(1, duration) * 0.25F;
    }
}
