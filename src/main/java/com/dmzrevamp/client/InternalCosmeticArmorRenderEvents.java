package com.dmzrevamp.client;

import com.dmzrevamp.DmzRevampMod;
import com.dmzrevamp.revamp.cosmetic.InternalCosmeticArmorRows;
import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderArmEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, value = Dist.CLIENT)
public final class InternalCosmeticArmorRenderEvents {
    private static final Map<Player, Deque<Runnable>> RESTORE_ACTIONS = new WeakHashMap<>();

    private InternalCosmeticArmorRenderEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        apply(event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        restore(event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onRenderPlayerCanceled(RenderPlayerEvent.Pre event) {
        if (event.isCanceled()) {
            restore(event.getEntity());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRenderHand(RenderHandEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            apply(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRenderHandRestore(RenderHandEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            restore(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRenderArm(RenderArmEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            apply(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRenderArmRestore(RenderArmEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            restore(player);
        }
    }

    private static void apply(Player player) {
        restore(player);
    }

    private static ItemStack replacementFor(Player player, EquipmentSlot slot) {
        if (InternalCosmeticArmorRows.isClientEnabled(player, InternalCosmeticArmorRows.ROW_EXTERNAL, slot)) {
            return InternalCosmeticArmorRows.getClientStack(player, InternalCosmeticArmorRows.ROW_EXTERNAL, slot);
        }
        if (InternalCosmeticArmorRows.isClientEnabled(player, InternalCosmeticArmorRows.ROW_FUSION, slot)) {
            return InternalCosmeticArmorRows.getClientStack(player, InternalCosmeticArmorRows.ROW_FUSION, slot);
        }
        return player.getInventory().armor.get(InternalCosmeticArmorRows.slotIndex(slot));
    }

    private static void restore(Player player) {
        Deque<Runnable> actions = RESTORE_ACTIONS.get(player);
        if (actions == null) {
            return;
        }
        Runnable action;
        while ((action = actions.poll()) != null) {
            action.run();
        }
    }
}
