package com.dmzrevamp.mixin.compat;

import com.dmzrevamp.revamp.strike.StrikeClashManager;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds Overhaul's configured late-input grace after Living World starts its one-shot rush check. */
@Pseudo
@Mixin(targets = "com.dmzlivingworld.world.StrikeClashCompat", remap = false)
public abstract class LivingWorldStrikeClashGraceMixin {
    @Inject(method = "begin", at = @At("TAIL"), remap = false, require = 1)
    private static void dmzrevamp$scheduleLateStrikeClashRetries(@Coerce Object fighter, CallbackInfo ci) {
        if (fighter instanceof LivingEntity entity) StrikeClashManager.scheduleLivingWorldGrace(entity);
    }

    @Redirect(method = "isStrikeRushing", at = @At(value = "INVOKE",
            target = "Lcom/dmzlivingworld/world/StrikeClashCompat;isRushing(Lcom/dmzlivingworld/entity/combat/LivingWorldSagasEntity;)Z"),
            remap = false, require = 1)
    private static boolean dmzrevamp$includeOverhaulGrace(@Coerce Object fighter) {
        return StrikeClashManager.isLivingWorldRushing(fighter);
    }
}
