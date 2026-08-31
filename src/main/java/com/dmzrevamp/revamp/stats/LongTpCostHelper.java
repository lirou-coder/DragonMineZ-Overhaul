package com.dmzrevamp.revamp.stats;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.StatsData;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Map;
import java.util.Locale;
import java.util.WeakHashMap;

public final class LongTpCostHelper {
    private static final DecimalFormat SCIENTIFIC_FORMATTER = new DecimalFormat("0.###E0", DecimalFormatSymbols.getInstance(Locale.US));
    private static final DecimalFormat FULL_FORMATTER = new DecimalFormat("#,##0.######", DecimalFormatSymbols.getInstance(Locale.US));
    private static final Map<StatsData, CachedCost> COST_CACHE = new WeakHashMap<>();

    private LongTpCostHelper() {
    }

    public static long calculateRecursiveCost(StatsData data, int amount) {
        if (data == null || amount <= 0) {
            return 0L;
        }
        CostContext context = context(data);
        if (!context.manualPurchasesEnabled) {
            return Long.MAX_VALUE;
        }

        int increase = Math.min(amount, Math.max(0, context.maxTotalStats - context.totalStats));
        CostKey key = new CostKey(increase, context);
        long cost = 0L;
        int start = 0;
        synchronized (COST_CACHE) {
            CachedCost cached = COST_CACHE.get(data);
            if (cached != null && cached.key.equals(key)) {
                return cached.cost;
            }
            if (cached != null
                    && cached.key.context.equals(context)
                    && cached.key.increase < increase) {
                cost = cached.cost;
                start = cached.key.increase;
            }
        }

        for (int i = start; i < increase; i++) {
            cost = saturatedAdd(cost, singleStatCost(context, context.totalStats + i));
        }
        synchronized (COST_CACHE) {
            COST_CACHE.put(data, new CachedCost(key, cost));
        }
        return cost;
    }

    public static int calculateAffordableIncrease(StatsData data, int maxIncrease, double availableTp) {
        if (data == null || maxIncrease <= 0 || !Double.isFinite(availableTp) || availableTp <= 0D) {
            return 0;
        }
        CostContext context = context(data);
        if (!context.manualPurchasesEnabled) {
            return 0;
        }

        long cost = 0L;
        int increase = 0;
        int allowedIncrease = Math.min(maxIncrease, Math.max(0, context.maxTotalStats - context.totalStats));
        while (increase < allowedIncrease) {
            int singleCost = singleStatCost(context, context.totalStats + increase);
            long nextCost = saturatedAdd(cost, singleCost);
            if (nextCost > availableTp) {
                break;
            }
            cost = nextCost;
            increase++;
        }
        synchronized (COST_CACHE) {
            COST_CACHE.put(data, new CachedCost(new CostKey(increase, context), cost));
        }
        return increase;
    }

    public static int toSaturatedInt(long value) {
        if (value <= 0L) {
            return 0;
        }
        return value >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    public static String formatLikeTp(long value) {
        long absoluteValue = Math.abs(value);
        if (absoluteValue >= 10_000_000_000L) {
            return SCIENTIFIC_FORMATTER.format(value);
        }
        return FULL_FORMATTER.format(value);
    }

    private static long saturatedAdd(long left, long right) {
        if (right > 0L && left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }

    private static CostContext context(StatsData data) {
        var serverConfig = ConfigManager.getServerConfig();
        var gameplay = serverConfig.getGameplay();
        var dynamicGrowth = serverConfig.getDynamicGrowth();
        return new CostContext(
                dynamicGrowth.isManualTpPurchasesEnabled(),
                data.getStats().getTotalStats(),
                data.getConfiguredMaxTotalStats(),
                gameplay.getGlobalTpCostMultiplier() * data.getRaceTpCostMultiplier(),
                gameplay.getMinTPCost(),
                gameplay.getMaxTPDiscount(),
                dynamicGrowth.getAttributeTpCostMultiplier()
        );
    }

    // Equivalent to StatsData#getSingleStatCost, with immutable config values captured once
    // for the whole batch instead of resolving every config entry for every purchased point.
    private static int singleStatCost(CostContext context, int totalStatValue) {
        double statValue = Math.max(0D, totalStatValue);
        double curveStart = context.maxTotalStats * 0.05D;
        double variableCost;
        if (curveStart <= 0D || statValue <= curveStart) {
            variableCost = statValue * 1.25D;
        } else {
            double costAtCurveStart = curveStart * 1.25D;
            double ratio = statValue / curveStart;
            variableCost = costAtCurveStart
                    + (costAtCurveStart / 0.7D) * (Math.pow(ratio, 0.7D) - 1D);
        }

        int discount = totalStatValue < context.maxTpDiscount
                ? context.maxTpDiscount - totalStatValue
                : 0;
        int cost = (int) ((context.minTpCost + variableCost) * context.combinedTpMultiplier) - discount;
        cost = Math.max(context.minTpCost, cost);
        if (context.attributeTpCostMultiplier > 1D) {
            double multiplied = Math.ceil(cost * context.attributeTpCostMultiplier);
            cost = multiplied >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) multiplied;
        }
        return Math.max(0, cost);
    }

    private record CostContext(
            boolean manualPurchasesEnabled,
            int totalStats,
            int maxTotalStats,
            double combinedTpMultiplier,
            int minTpCost,
            int maxTpDiscount,
            double attributeTpCostMultiplier
    ) {
    }

    private record CostKey(int increase, CostContext context) {
    }

    private record CachedCost(CostKey key, long cost) {
    }
}
