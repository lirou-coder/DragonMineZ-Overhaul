package com.dmzrevamp.mixin;

import com.dragonminez.common.init.entities.ki.KiExplosionEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Exposes DMZ's own protected/grief-rule-aware Explosion crater routine. */
@Mixin(KiExplosionEntity.class)
public interface KiExplosionEntityAccessor {
    @Invoker("createCrater")
    void dmzrevamp$createCrater(float radius);
}
