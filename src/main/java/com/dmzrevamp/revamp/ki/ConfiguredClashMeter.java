package com.dmzrevamp.revamp.ki;

import com.dmzrevamp.config.KiClashConfigured;
import com.dmzrevamp.config.StrikeClashConfigured;
import com.dragonminez.common.combat.clash.ClashMeter;
import net.minecraft.util.Mth;

/** Builds DMZ 2.2 clash cycles without changing their deterministic seed protocol. */
public final class ConfiguredClashMeter {
    private static final int BASE_CYCLE_MIN_TICKS = 44;
    private static final int BASE_CYCLE_MAX_TICKS = 64;
    private static final float TARGET_PROGRESS_MIN = 0.42F;
    private static final float TARGET_PROGRESS_MAX = 0.86F;
    private static final float BASE_HALF_WIDTH_MIN = 0.065F;
    private static final float BASE_HALF_WIDTH_MAX = 0.09F;
    private static final int MAX_CYCLES = 128;

    private ConfiguredClashMeter() {
    }

    public static ClashMeter.Sample sampleKi(long seed, float time) {
        KiClashConfigured.Config config = KiClashConfigured.get();
        return sample(seed, time, config.meterSpeedMultiplier,
                config.goodAreaSizeMultiplier, config.perfectAreaFraction,
                config.goodMinimumEfficiency);
    }

    public static ClashMeter.Sample sampleStrike(long seed, float time, float participantAreaMultiplier) {
        StrikeClashConfigured.Config config = StrikeClashConfigured.get();
        return sampleStrike(seed, time, participantAreaMultiplier, config.meterSpeedMultiplier,
                config.goodAreaSizeMultiplier, config.perfectAreaFraction, config.goodMinimumEfficiency);
    }

    public static ClashMeter.Sample sampleStrike(long seed, float time, float participantAreaMultiplier,
                                                  float speedMultiplier, float areaSizeMultiplier,
                                                  float perfectFraction, float minimumGoodEfficiency) {
        return sample(seed, time, speedMultiplier,
                areaSizeMultiplier * Math.max(0.01F, participantAreaMultiplier),
                perfectFraction, minimumGoodEfficiency);
    }

    public static ClashMeter.Sample sampleKi(long seed, float time, float speedMultiplier,
                                              float areaSizeMultiplier, float perfectFraction,
                                              float minimumGoodEfficiency) {
        return sample(seed, time, speedMultiplier, areaSizeMultiplier,
                perfectFraction, minimumGoodEfficiency);
    }

    private static ClashMeter.Sample sample(long seed, float time, float speedMultiplier,
                                             float areaMultiplier, float perfectFraction,
                                             float minimumGoodEfficiency) {
        ClashMeter.Cycle cycle = cycleAt(seed, time, speedMultiplier, areaMultiplier);
        float marker = cycle.markerAt(time);
        float distance = Math.abs(marker - cycle.center()) / cycle.halfWidth();
        ClashMeter.Grade grade;
        float efficiency;
        if (distance <= perfectFraction) {
            grade = ClashMeter.Grade.PERFECT;
            efficiency = 1.0F;
        } else if (distance <= 1.0F) {
            grade = ClashMeter.Grade.GOOD;
            float t = (distance - perfectFraction) / Math.max(0.0001F, 1.0F - perfectFraction);
            efficiency = 1.0F - (1.0F - minimumGoodEfficiency) * t;
        } else {
            grade = ClashMeter.Grade.MISS;
            efficiency = 0.0F;
        }
        return new ClashMeter.Sample(cycle, marker, distance, grade, efficiency);
    }

    private static ClashMeter.Cycle cycleAt(long seed, float time, float speedMultiplier, float areaMultiplier) {
        int minimumDuration = Math.max(1, Math.round(BASE_CYCLE_MIN_TICKS / speedMultiplier));
        int maximumDuration = Math.max(minimumDuration, Math.round(BASE_CYCLE_MAX_TICKS / speedMultiplier));
        int start = 0;
        ClashMeter.Cycle cycle = null;
        for (int index = 0; index < MAX_CYCLES; index++) {
            float durationRoll = ClashMeter.hash01(seed, index, 0);
            int duration = minimumDuration + Math.round(durationRoll * (maximumDuration - minimumDuration));
            float progress = Mth.lerp(ClashMeter.hash01(seed, index, 1),
                    TARGET_PROGRESS_MIN, TARGET_PROGRESS_MAX);
            boolean reversed = (index & 1) == 1;
            float center = reversed ? 1.0F - progress : progress;
            float baseHalfWidth = Mth.lerp(ClashMeter.hash01(seed, index, 2),
                    BASE_HALF_WIDTH_MIN, BASE_HALF_WIDTH_MAX);
            float maximumHalfWidth = Math.max(0.001F, Math.min(center, 1.0F - center));
            float halfWidth = Mth.clamp(baseHalfWidth * areaMultiplier, 0.001F, maximumHalfWidth);
            cycle = new ClashMeter.Cycle(index, start, duration, center, halfWidth, reversed);
            if (time < cycle.endTick()) return cycle;
            start = cycle.endTick();
        }
        return cycle;
    }
}
