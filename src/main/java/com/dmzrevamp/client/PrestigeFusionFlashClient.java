package com.dmzrevamp.client;

import com.dragonminez.client.render.effects.AuraRenderer;
import net.minecraft.client.Minecraft;

import java.lang.reflect.Field;
import java.util.Map;

public final class PrestigeFusionFlashClient {
    private PrestigeFusionFlashClient() {
    }

    @SuppressWarnings("unchecked")
    public static void trigger(int entityId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.level.getEntity(entityId) == null) return;
        try {
            Field startsField = AuraRenderer.class.getDeclaredField("FUSION_START_TIME");
            startsField.setAccessible(true);
            Map<Integer, Long> starts = (Map<Integer, Long>) startsField.get(null);
            starts.put(entityId, minecraft.level.getGameTime());
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
