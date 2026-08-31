package com.dmzrevamp.mixin.client;

import com.dragonminez.client.flight.CombatFlightHandler;
import com.dragonminez.common.config.CombatConfig;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CombatFlightHandler.class)
public abstract class CombatFlightHandlerRevampMixin {
    @Redirect(
            method = "handleDoubleTaps",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dragonminez/client/flight/CombatFlightHandler;applyImpulse(Lnet/minecraft/client/player/LocalPlayer;ILnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lcom/dragonminez/common/config/CombatConfig;FI)V"
            ),
            remap = false
    )
    private static void dmzrevamp$blockHorizontalMovementKeyImpulse(LocalPlayer player, int direction, Vec3 forward, Vec3 right, CombatConfig config, float flySpeedScale, int flyLevel) {
        if (direction >= 4) {
            dmzrevamp$applyImpulse(player, direction, forward, right, config, flySpeedScale, flyLevel);
        }
    }

    @org.spongepowered.asm.mixin.gen.Invoker(value = "applyImpulse", remap = false)
    private static void dmzrevamp$applyImpulse(LocalPlayer player, int direction, Vec3 forward, Vec3 right, CombatConfig config, float flySpeedScale, int flyLevel) {
        throw new AssertionError();
    }
}
