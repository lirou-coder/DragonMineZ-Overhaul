package com.dmzrevamp.mixin.client;

import com.dragonminez.client.events.LockOnEvent;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = LockOnEvent.class, remap = false)
public interface LockOnEventAccessor {
    @Accessor("lockedTarget")
    static LivingEntity dmzrevamp$getLockedTarget() {
        throw new AssertionError();
    }

    @Accessor("lockedTarget")
    static void dmzrevamp$setLockedTarget(LivingEntity target) {
        throw new AssertionError();
    }
}
