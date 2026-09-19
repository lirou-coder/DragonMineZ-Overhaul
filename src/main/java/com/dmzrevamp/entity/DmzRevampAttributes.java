package com.dmzrevamp.entity;

import com.dmzrevamp.DmzRevampMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class DmzRevampAttributes {
    private static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(ForgeRegistries.ATTRIBUTES, DmzRevampMod.MODID);

    public static final RegistryObject<Attribute> MOB_DEFENSE = ATTRIBUTES.register("mob_defense",
            () -> new RangedAttribute("attribute.name.dmzrevamp.mob_defense", 1D, 0D, Double.MAX_VALUE).setSyncable(true));

    private DmzRevampAttributes() {}

    public static void register(IEventBus bus) {
        ATTRIBUTES.register(bus);
    }

    @Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() {}

        @SubscribeEvent
        public static void addToEveryLivingMob(EntityAttributeModificationEvent event) {
            for (EntityType<?> type : ForgeRegistries.ENTITY_TYPES.getValues()) {
                if (type == EntityType.PLAYER) continue;
                try {
                    @SuppressWarnings("unchecked") EntityType<? extends LivingEntity> livingType =
                            (EntityType<? extends LivingEntity>) type;
                    event.add(livingType, MOB_DEFENSE.get());
                } catch (RuntimeException ignored) {
                    // Non-living types have no attribute supplier.
                }
            }
        }
    }
}
