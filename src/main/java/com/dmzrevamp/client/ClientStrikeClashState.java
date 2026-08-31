package com.dmzrevamp.client;

import net.minecraft.client.Minecraft;

import java.util.HashSet;
import java.util.Set;

public final class ClientStrikeClashState {
    private static volatile boolean active;
    private static final Set<Integer> CLASHING_ENTITIES = new HashSet<>();

    private ClientStrikeClashState() {
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean isEntityActive(int entityId) {
        return CLASHING_ENTITIES.contains(entityId);
    }

    public static void setEntityActive(int entityId, boolean entityActive) {
        if (entityActive) CLASHING_ENTITIES.add(entityId);
        else CLASHING_ENTITIES.remove(entityId);

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.getId() == entityId) {
            active = entityActive;
        }
    }

    public static void clear() {
        active = false;
        CLASHING_ENTITIES.clear();
    }
}
