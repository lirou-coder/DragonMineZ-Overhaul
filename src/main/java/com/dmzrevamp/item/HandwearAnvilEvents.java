package com.dmzrevamp.item;

import com.dmzrevamp.DmzRevampMod;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HandwearAnvilEvents {
    private HandwearAnvilEvents() {}

    @SubscribeEvent
    public static void repairByTenths(AnvilUpdateEvent event) {
        ItemStack damaged = event.getLeft();
        if (!(damaged.getItem() instanceof HandwearItem handwear) || !damaged.isDamaged()
                || !handwear.isValidRepairItem(damaged, event.getRight())) return;
        int perItem = Math.max(1, (int) Math.ceil(damaged.getMaxDamage() * 0.10D));
        int needed = Math.max(1, (int) Math.ceil(damaged.getDamageValue() / (double) perItem));
        int used = Math.min(event.getRight().getCount(), needed);
        ItemStack output = damaged.copy();
        output.setDamageValue(Math.max(0, damaged.getDamageValue() - perItem * used));
        event.setOutput(output);
        event.setMaterialCost(used);
        event.setCost(used);
    }
}
