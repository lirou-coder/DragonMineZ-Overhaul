package com.dmzrevamp.mixin.client;

import com.dragonminez.client.render.effects.MajinAbsorptionTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MajinAbsorptionTracker.class)
public abstract class MajinAbsorptionTrackerRevampMixin {
    @Redirect(
            method = "onClientTick",
            at = @At(value = "INVOKE", target = "Ljava/lang/String;equals(Ljava/lang/Object;)Z"),
            remap = false,
            require = 0
    )
    private static boolean dmzrevamp$acceptRevampMajinAbsorption(String expected, Object actual) {
        if ("majin".equals(expected) && actual instanceof String skillId) {
            return "majin".equals(skillId) || "majinrevamp".equals(skillId);
        }
        return expected.equals(actual);
    }
}
