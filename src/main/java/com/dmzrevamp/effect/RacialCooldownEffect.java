package com.dmzrevamp.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class RacialCooldownEffect extends MobEffect {
    // Builds a visible neutral cooldown marker for racial skills that are waiting before they can trigger again.
    public RacialCooldownEffect() {
        super(MobEffectCategory.NEUTRAL, 0x7A7A7A);
    }
}
