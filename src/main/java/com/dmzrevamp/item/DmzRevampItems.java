package com.dmzrevamp.item;

import com.dmzrevamp.DmzRevampMod;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class DmzRevampItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, DmzRevampMod.MODID);

    public static final RegistryObject<Item> GLOVES = ITEMS.register("gloves",
            () -> new HandwearItem(HandwearType.GLOVES, new Item.Properties().stacksTo(1).durability(Items.IRON_SWORD.getMaxDamage() * 10).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> WRISTBANDS = ITEMS.register("wristbands",
            () -> new HandwearItem(HandwearType.WRISTBANDS, new Item.Properties().stacksTo(1).durability(Items.IRON_SWORD.getMaxDamage() * 10).rarity(Rarity.UNCOMMON)));

    private DmzRevampItems() {
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        bus.register(DmzRevampItems.class);
    }

    @SubscribeEvent
    public static void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(GLOVES);
            event.accept(WRISTBANDS);
        }
    }
}
