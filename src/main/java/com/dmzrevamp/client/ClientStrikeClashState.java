package com.dmzrevamp.client;

import net.minecraft.client.Minecraft;

import java.util.HashSet;
import java.util.Set;

public final class ClientStrikeClashState {
    private static volatile boolean active;
    private static volatile float goodAreaMultiplier = 1.0F;
    private static volatile float meterSpeedMultiplier = 1.0F;
    private static volatile float areaSizeMultiplier = 1.0F;
    private static volatile float perfectFraction = 0.35F;
    private static volatile float minimumGoodEfficiency = 0.5F;
    private static final Set<Integer> CLASHING_ENTITIES = new HashSet<>();

    private ClientStrikeClashState() {
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean isEntityActive(int entityId) {
        return CLASHING_ENTITIES.contains(entityId);
    }

    public static float goodAreaMultiplier() {
        return goodAreaMultiplier;
    }

    public static float meterSpeedMultiplier() { return meterSpeedMultiplier; }
    public static float areaSizeMultiplier() { return areaSizeMultiplier; }
    public static float perfectFraction() { return perfectFraction; }
    public static float minimumGoodEfficiency() { return minimumGoodEfficiency; }

    public static void setEntityActive(int entityId, boolean entityActive, float participantAreaMultiplier,
                                       float speedMultiplier, float areaMultiplier,
                                       float perfect, float minimumGood) {
        if (entityActive) CLASHING_ENTITIES.add(entityId);
        else CLASHING_ENTITIES.remove(entityId);

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.getId() == entityId) {
            active = entityActive;
            goodAreaMultiplier = entityActive && Float.isFinite(participantAreaMultiplier)
                    ? Math.max(0.01F, participantAreaMultiplier) : 1.0F;
            meterSpeedMultiplier = finitePositive(speedMultiplier, 1.0F);
            areaSizeMultiplier = finitePositive(areaMultiplier, 1.0F);
            perfectFraction = finiteUnit(perfect, 0.35F);
            minimumGoodEfficiency = finiteUnit(minimumGood, 0.5F);
        }
    }

    public static void clear() {
        active = false;
        goodAreaMultiplier = 1.0F;
        CLASHING_ENTITIES.clear();
    }

    private static float finitePositive(float value, float fallback) {
        return Float.isFinite(value) && value > 0F ? value : fallback;
    }

    private static float finiteUnit(float value, float fallback) {
        return Float.isFinite(value) ? Math.max(0F, Math.min(1F, value)) : fallback;
    }
}
