package com.dmzrevamp.revamp.stats;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.StatsData;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

public final class LongTpCostHelper {
    private static final DecimalFormat SCIENTIFIC_FORMATTER = new DecimalFormat("0.###E0", DecimalFormatSymbols.getInstance(Locale.US));
    private static final DecimalFormat FULL_FORMATTER = new DecimalFormat("#,##0.######", DecimalFormatSymbols.getInstance(Locale.US));
    private static final Map<StatsData, CachedCosts> COST_CACHE = new WeakHashMap<>();

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
        synchronized (COST_CACHE) {
            return cumulativeCost(cacheFor(data, context, increase), increase);
        }
    }

    public static int calculateAffordableIncrease(StatsData data, int maxIncrease, double availableTp) {
        if (data == null || maxIncrease <= 0 || !Double.isFinite(availableTp) || availableTp <= 0D) {
            return 0;
        }
        CostContext context = context(data);
        if (!context.manualPurchasesEnabled) {
            return 0;
        }

        int allowedIncrease = Math.min(maxIncrease, Math.max(0, context.maxTotalStats - context.totalStats));
        if (allowedIncrease <= 0) return 0;
        synchronized (COST_CACHE) {
            long[] costs = cacheFor(data, context, 1);
            if (cumulativeCost(costs, 1) > availableTp) return 0;

            int low = 1;
            int high = 1;
            while (high < allowedIncrease) {
                int next = (int) Math.min(allowedIncrease, (long) high * 2L);
                costs = cacheFor(data, context, next);
                if (cumulativeCost(costs, next) > availableTp) {
                    high = next;
                    break;
                }
                low = high = next;
            }
            if (low == allowedIncrease) return low;

            while (low < high) {
                int mid = low + (high - low + 1) / 2;
                costs = cacheFor(data, context, mid);
                if (cumulativeCost(costs, mid) <= availableTp) low = mid;
                else high = mid - 1;
            }
            return low;
        }
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
                data.getTpCostTotalStats(),
                gameplay.getGlobalTpCostMultiplier() * data.getRaceTpCostMultiplier(),
                gameplay.getMinTPCost(),
                gameplay.getMaxTPDiscount(),
                dynamicGrowth.getAttributeTpCostMultiplier()
        );
    }

    private static long[] cacheFor(StatsData data, CostContext context, int requestedIncrease) {
        CachedCosts cached = COST_CACHE.get(data);
        if (cached == null || !cached.context.equals(context)) {
            cached = new CachedCosts(context, new long[]{0L});
            COST_CACHE.put(data, cached);
        }
        int requiredLength = requestedIncrease + 1;
        if (cached.prefix.length < requiredLength) {
            long[] expanded = java.util.Arrays.copyOf(cached.prefix, requiredLength);
            for (int i = cached.prefix.length - 1; i < requestedIncrease; i++) {
                expanded[i + 1] = saturatedAdd(expanded[i], singleStatCost(context, context.costTotalStats + i));
            }
            cached.prefix = expanded;
        }
        return cached.prefix;
    }

    private static long cumulativeCost(long[] prefix, int increase) {
        return prefix[Math.min(Math.max(0, increase), prefix.length - 1)];
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
            int costTotalStats,
            double combinedTpMultiplier,
            int minTpCost,
            int maxTpDiscount,
            double attributeTpCostMultiplier
    ) {
    }

    private static final class CachedCosts {
        private final CostContext context;
        private long[] prefix;

        private CachedCosts(CostContext context, long[] prefix) {
            this.context = context;
            this.prefix = prefix;
        }
    }
}
