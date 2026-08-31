package com.dmzrevamp.effect;

import com.dmzrevamp.DmzRevampMod;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class DmzRevampEffects {
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, DmzRevampMod.MODID);

    public static final RegistryObject<MobEffect> RACIAL_COOLDOWN = EFFECTS.register("racial_cooldown", RacialCooldownEffect::new);
    public static final RegistryObject<MobEffect> ADRENALINE_COOLDOWN = EFFECTS.register("adrenaline_cooldown", RacialCooldownEffect::new);
    public static final RegistryObject<MobEffect> REGENERATION_COOLDOWN = EFFECTS.register("regeneration_cooldown", RacialCooldownEffect::new);

    private DmzRevampEffects() {
    }

    // Adds this mod's remaining effects to Forge's effect registry.
    public static void register(IEventBus eventBus) {
        EFFECTS.register(eventBus);
    }
}
