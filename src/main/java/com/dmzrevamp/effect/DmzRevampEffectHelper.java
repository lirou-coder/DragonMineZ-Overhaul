package com.dmzrevamp.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class DmzRevampEffectHelper {
    // This helper only creates standardized effect instances, so it should not be instantiated.
    private DmzRevampEffectHelper() {
    }

    // Creates a normal effect instance that cannot be removed by drinking milk or other curative items.
    public static MobEffectInstance create(MobEffect effect, int duration, int amplifier) {
        return create(effect, duration, amplifier, false, false, true);
    }

    // Creates a configurable effect instance and strips curative items so DMZ cooldown/status effects stay controlled by code.
    public static MobEffectInstance create(MobEffect effect, int duration, int amplifier, boolean ambient, boolean visible, boolean showIcon) {
        MobEffectInstance instance = new MobEffectInstance(effect, duration, amplifier, ambient, visible, showIcon);
        instance.setCurativeItems(List.of(ItemStack.EMPTY));
        return instance;
    }

    // Creates a permanent effect instance that still uses the same non-curable behavior as temporary Overhaul effects.
    public static MobEffectInstance createInfinite(MobEffect effect, int amplifier, boolean ambient, boolean visible, boolean showIcon) {
        MobEffectInstance instance = new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION, amplifier, ambient, visible, showIcon);
        instance.setCurativeItems(List.of(ItemStack.EMPTY));
        return instance;
    }
}
