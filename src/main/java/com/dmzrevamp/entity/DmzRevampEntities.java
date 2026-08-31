package com.dmzrevamp.entity;

import com.dmzrevamp.DmzRevampMod;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class DmzRevampEntities {
    private static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, DmzRevampMod.MODID);

    public static final RegistryObject<EntityType<SagaGogetaEntity.Base>> SAGA_GOGETA_BASE = ENTITIES.register(
            "saga_gogeta_base", () -> EntityType.Builder.of(SagaGogetaEntity.Base::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.8F).clientTrackingRange(10).build("saga_gogeta_base"));

    public static final RegistryObject<EntityType<SagaGogetaEntity.Ssj>> SAGA_GOGETA_SSJ = ENTITIES.register(
            "saga_gogeta_ssj", () -> EntityType.Builder.of(SagaGogetaEntity.Ssj::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.8F).clientTrackingRange(10).build("saga_gogeta_ssj"));

    private DmzRevampEntities() {}

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }

    @Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Attributes {
        private Attributes() {}

        @SubscribeEvent
        public static void register(EntityAttributeCreationEvent event) {
            event.put(SAGA_GOGETA_BASE.get(), DBSagasEntity.createAttributes().build());
            event.put(SAGA_GOGETA_SSJ.get(), DBSagasEntity.createAttributes().build());
        }
    }
}
