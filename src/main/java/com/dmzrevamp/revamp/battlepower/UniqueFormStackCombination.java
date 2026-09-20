package com.dmzrevamp.revamp.battlepower;

import com.dmzrevamp.config.CustomBattlePowerConfig;
import com.dmzrevamp.config.DmzRevampConfig;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.StatsData;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** BP-balanced Form + Stack Form multiplier combination without mutating player data. */
public final class UniqueFormStackCombination {
    private static final String[] STATS = {"STR", "SKP", "DEF", "STM", "VIT", "PWR", "ENE"};
    private static final int SEARCH_ITERATIONS = 18;
    private static final double BP_TOLERANCE = 1.0E-6D;
    private static final ThreadLocal<Evaluation> EVALUATION = new ThreadLocal<>();
    private static final Map<StatsData, CachedFactor> CACHE = Collections.synchronizedMap(new WeakHashMap<>());

    private UniqueFormStackCombination() {
    }

    public static double replaceFormAndStackInTotal(StatsData data, String stat, double original) {
        if (!enabledFor(data)) return original;

        Evaluation evaluation = EVALUATION.get();
        double form = saneMultiplier(data.getFormMultiplier(stat));
        double stack = saneMultiplier(data.getStackFormMultiplier(stat));
        double formAndStack;
        if (evaluation != null && evaluation.data == data) {
            formAndStack = switch (evaluation.mode) {
                case BASE -> 1D;
                case FORM -> form;
                case STACK -> stack;
                case INTERPOLATED -> interpolate(form, stack, evaluation.factor);
            };
        } else {
            formAndStack = interpolate(form, stack, factor(data));
        }
        double dmzFormAndStack = dmzCombinesByMultiplication() ? form * stack : form + stack - 1D;
        double result;
        if (dmzCombinesByMultiplication()) {
            if (!Double.isFinite(dmzFormAndStack) || Math.abs(dmzFormAndStack) < 1.0E-12D) return original;
            result = original * formAndStack / dmzFormAndStack;
        } else {
            result = original + formAndStack - dmzFormAndStack;
        }
        return Double.isFinite(result) ? result : original;
    }

    public static double formAndStackMultiplier(StatsData data, String stat) {
        double form = saneMultiplier(data.getFormMultiplier(stat));
        double stack = saneMultiplier(data.getStackFormMultiplier(stat));
        if (!enabledFor(data)) return dmzCombinesByMultiplication() ? form * stack : form + stack - 1D;
        Evaluation evaluation = EVALUATION.get();
        if (evaluation != null && evaluation.data == data) {
            return switch (evaluation.mode) {
                case BASE -> 1D;
                case FORM -> form;
                case STACK -> stack;
                case INTERPOLATED -> interpolate(form, stack, evaluation.factor);
            };
        }
        return interpolate(form, stack, factor(data));
    }

    public static void clearCache() {
        CACHE.clear();
    }

    public static boolean enabledFor(StatsData data) {
        return data != null
                && DmzRevampConfig.OVERHAUL_UNIQUE_STACK_MODE.get()
                && data.getCharacter() != null
                && data.getCharacter().hasActiveForm()
                && data.getCharacter().hasActiveStackForm();
    }

    private static double factor(StatsData data) {
        long signature = signature(data);
        CachedFactor cached = CACHE.get(data);
        if (cached != null && cached.signature == signature) return cached.factor;

        double solved = solve(data);
        if (!Double.isFinite(solved)) solved = dmzCombinesByMultiplication() ? 1D : 0D;
        solved = clamp01(solved);
        CACHE.put(data, new CachedFactor(signature, solved));
        return solved;
    }

    private static double solve(StatsData data) {
        if (EVALUATION.get() != null) return Double.NaN;
        double base = evaluate(data, Mode.BASE, 0D);
        double form = evaluate(data, Mode.FORM, 0D);
        double stack = evaluate(data, Mode.STACK, 0D);
        if (!positiveFinite(base) || !finiteNonNegative(form) || !finiteNonNegative(stack)) return Double.NaN;

        double target = form * stack / base;
        if (!finiteNonNegative(target)) return Double.NaN;

        double low = 0D;
        double high = 1D;
        double lowBp = evaluate(data, Mode.INTERPOLATED, low);
        double highBp = evaluate(data, Mode.INTERPOLATED, high);
        if (!finiteNonNegative(lowBp) || !finiteNonNegative(highBp)) return Double.NaN;

        double bestFactor = distance(lowBp, target) <= distance(highBp, target) ? low : high;
        double bestDistance = Math.min(distance(lowBp, target), distance(highBp, target));
        double minimum = Math.min(lowBp, highBp);
        double maximum = Math.max(lowBp, highBp);
        if (target <= minimum || target >= maximum || nearlyEqual(lowBp, highBp)) return bestFactor;

        boolean increasing = highBp > lowBp;
        for (int iteration = 0; iteration < SEARCH_ITERATIONS; iteration++) {
            double middle = (low + high) * 0.5D;
            double middleBp = evaluate(data, Mode.INTERPOLATED, middle);
            if (!finiteNonNegative(middleBp)) break;
            double currentDistance = distance(middleBp, target);
            if (currentDistance < bestDistance) {
                bestDistance = currentDistance;
                bestFactor = middle;
            }
            if (currentDistance <= Math.max(1D, target) * BP_TOLERANCE) break;
            if ((middleBp < target) == increasing) low = middle;
            else high = middle;
        }
        return bestFactor;
    }

    private static double evaluate(StatsData data, Mode mode, double factor) {
        EVALUATION.set(new Evaluation(data, mode, factor));
        try {
            return CustomBattlePowerCalculator.calculateFinitePlayerBattlePower(data);
        } catch (RuntimeException ignored) {
            return Double.NaN;
        } finally {
            EVALUATION.remove();
        }
    }

    private static boolean dmzCombinesByMultiplication() {
        var server = ConfigManager.getServerConfig();
        return server != null && server.getGameplay() != null
                && Boolean.TRUE.equals(server.getGameplay().getMultiplicationInsteadOfAdditionForMultipliers());
    }

    private static double interpolate(double form, double stack, double factor) {
        double additive = form + stack - 1D;
        double multiplicative = form * stack;
        double result = additive + clamp01(factor) * (multiplicative - additive);
        return Double.isFinite(result) ? result : 1D;
    }

    private static long signature(StatsData data) {
        long hash = 17L;
        hash = mix(hash, data.getCharacter().getRaceName());
        hash = mix(hash, data.getCharacter().getActiveFormGroup());
        hash = mix(hash, data.getCharacter().getActiveForm());
        hash = mix(hash, data.getCharacter().getActiveStackFormGroup());
        hash = mix(hash, data.getCharacter().getActiveStackForm());
        hash = mix(hash, data.getStats().getStrength());
        hash = mix(hash, data.getStats().getStrikePower());
        hash = mix(hash, data.getStats().getResistance());
        hash = mix(hash, data.getStats().getVitality());
        hash = mix(hash, data.getStats().getKiPower());
        hash = mix(hash, data.getStats().getEnergy());
        hash = mix(hash, data.getResources().getPowerRelease());
        for (String stat : STATS) {
            hash = mix(hash, data.getFormMultiplier(stat));
            hash = mix(hash, data.getStackFormMultiplier(stat));
            hash = mix(hash, data.getEffectsMultiplier(stat));
            hash = mix(hash, data.getSecondaryStatEffects().getMultiplier(stat));
            hash = mix(hash, data.getBonusStats().getBonuses(stat).hashCode());
        }
        CustomBattlePowerConfig.Config config = CustomBattlePowerConfig.get();
        hash = mix(hash, System.identityHashCode(config));
        hash = mix(hash, config.referenceMultiplier);
        hash = mix(hash, config.totalStatsDivisor);
        hash = mix(hash, config.exponent);
        for (CustomBattlePowerConfig.StatRule rule : config.playerStats.values()) {
            hash = mix(hash, rule.enabled ? 1 : 0);
            hash = mix(hash, rule.weight);
        }
        hash = mix(hash, dmzCombinesByMultiplication() ? 1 : 0);
        return hash;
    }

    private static long mix(long hash, Object value) {
        return 31L * hash + (value == null ? 0 : value.hashCode());
    }

    private static long mix(long hash, double value) {
        return 31L * hash + Double.doubleToLongBits(value);
    }

    private static double saneMultiplier(double value) {
        return Double.isFinite(value) ? value : 1D;
    }

    private static boolean positiveFinite(double value) {
        return Double.isFinite(value) && value > 0D;
    }

    private static boolean finiteNonNegative(double value) {
        return Double.isFinite(value) && value >= 0D;
    }

    private static boolean nearlyEqual(double first, double second) {
        return Math.abs(first - second) <= Math.max(1D, Math.max(Math.abs(first), Math.abs(second))) * 1.0E-12D;
    }

    private static double distance(double value, double target) {
        return Math.abs(value - target);
    }

    private static double clamp01(double value) {
        return Math.max(0D, Math.min(1D, value));
    }

    private enum Mode { BASE, FORM, STACK, INTERPOLATED }

    private record Evaluation(StatsData data, Mode mode, double factor) {
    }

    private record CachedFactor(long signature, double factor) {
    }
}
