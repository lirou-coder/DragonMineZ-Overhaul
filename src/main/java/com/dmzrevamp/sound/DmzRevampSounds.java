package com.dmzrevamp.sound;

import com.dmzrevamp.DmzRevampMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class DmzRevampSounds {
    private static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, DmzRevampMod.MODID);

    public static final RegistryObject<SoundEvent> GRAB = register("grab");
    public static final RegistryObject<SoundEvent> ARM_SNAP = register("arm_snap");
    public static final RegistryObject<SoundEvent> ARM_REGEN = register("arm_regen");

    private DmzRevampSounds() {
    }

    public static void register(IEventBus eventBus) {
        SOUNDS.register(eventBus);
    }

    private static RegistryObject<SoundEvent> register(String id) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(DmzRevampMod.MODID, id);
        return SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(location));
    }
}
