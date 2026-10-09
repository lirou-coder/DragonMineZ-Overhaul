package com.dmzrevamp.mixin;

import com.dragonminez.common.init.entities.ki.KiLaserEntity;
import net.minecraft.network.syncher.EntityDataAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = KiLaserEntity.class, remap = false)
public interface KiLaserEntityAccessor {
    @Accessor("FIXED_YAW")
    static EntityDataAccessor<Float> dmzrevamp$getFixedYawKey() { throw new AssertionError(); }

    @Accessor("FIXED_PITCH")
    static EntityDataAccessor<Float> dmzrevamp$getFixedPitchKey() { throw new AssertionError(); }
}
