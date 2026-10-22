package com.dmzrevamp.client;

import com.dmzrevamp.DmzRevampMod;
import com.dmzrevamp.network.DmzRevampNetwork;
import com.dmzrevamp.network.LegacyComboAttackC2SPacket;
import com.dragonminez.client.util.KeyBinds;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class LegacyComboInputEvents {
    private LegacyComboInputEvents() {
    }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT || event.getAction() != GLFW.GLFW_PRESS) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null || !KeyBinds.SECOND_FUNCTION_KEY.isDown()) return;

        StatsProvider.get(StatsCapability.INSTANCE, minecraft.player).ifPresent(data -> {
            if (!data.getStatus().isHasCreatedCharacter()
                    || data.getStatus().isStunned()
                    || data.getStatus().isBlocking()
                    || data.getCooldowns().hasCooldown("ComboAttackCooldown")) return;
            DmzRevampNetwork.CHANNEL.sendToServer(new LegacyComboAttackC2SPacket());
        });
    }
}
