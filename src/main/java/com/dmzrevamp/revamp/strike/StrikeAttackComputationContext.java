package com.dmzrevamp.revamp.strike;

/**
 * Thread-local marker used only while DragonMineZ is building an ActiveStrike.
 * This lets the Overhaul redirect Strike Attack power to STR/Melee without
 * targeting compiler-generated lambda methods in StrikeAttackHandler.
 */
public final class StrikeAttackComputationContext {
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

    private StrikeAttackComputationContext() {
    }

    public static void enter() {
        DEPTH.set(DEPTH.get() + 1);
    }

    public static void exit() {
        int depth = DEPTH.get() - 1;
        if (depth <= 0) {
            DEPTH.remove();
        } else {
            DEPTH.set(depth);
        }
    }

    public static boolean isActive() {
        return DEPTH.get() > 0;
    }
}
