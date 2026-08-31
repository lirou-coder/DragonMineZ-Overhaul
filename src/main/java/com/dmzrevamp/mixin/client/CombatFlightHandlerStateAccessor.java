package com.dmzrevamp.mixin.client;

import com.dragonminez.client.flight.CombatFlightHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CombatFlightHandler.class)
public interface CombatFlightHandlerStateAccessor {
    @Accessor(value = "sustainedDir", remap = false)
    static void dmzrevamp$setSustainedDir(int direction) {
        throw new AssertionError();
    }
}
