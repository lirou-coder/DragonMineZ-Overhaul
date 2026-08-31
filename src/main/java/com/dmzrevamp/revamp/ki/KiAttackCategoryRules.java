package com.dmzrevamp.revamp.ki;

import com.dragonminez.common.stats.techniques.KiAttackData;

public final class KiAttackCategoryRules {
    private KiAttackCategoryRules() {
    }

    public static KiAttackCategory classify(KiAttackData data) {
        int effectCount = activeEffectCount(data);
        if (effectCount <= 0) {
            return KiAttackCategory.BASIC;
        }

        int tierUps = 0;
        if (data.getDamageMultiplier() > 1.0F) {
            tierUps++;
        }
        if (effectCount > 1) {
            tierUps++;
        }
        if (effectCount >= 3) {
            tierUps++;
        }
        if (firesMultipleProjectiles(data)) {
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

    public static float secondaryWeight(float intensity, int duration) {
        return (Math.max(0.0F, intensity) / 50.0F) * Math.max(1, duration) * 0.25F;
    }

    public static KiAttackCategory classifyPreview(float damageMultiplier, int effectCount, int projectileCount) {
        if (effectCount <= 0) {
            return KiAttackCategory.BASIC;
        }
        int tierUps = 0;
        if (damageMultiplier > 1.0F) {
            tierUps++;
        }
        if (effectCount > 1) {
            tierUps++;
        }
        if (effectCount >= 3) {
            tierUps++;
        }
        if (projectileCount > 1) {
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

    public static int activeEffectCount(KiAttackData data) {
        int count = 0;
        if (data.getSecondaryEffectType() != KiAttackData.SecondaryEffectType.NONE) {
            count++;
        }
        if (data instanceof RevampKiAttackData revamp) {
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

    private static boolean firesMultipleProjectiles(KiAttackData data) {
        return data instanceof RevampKiAttackData revamp && revamp.dmzrevamp$getMultiCastCount() > 1;
    }
}
