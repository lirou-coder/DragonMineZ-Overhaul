package com.dmzrevamp.mixin;

import com.dragonminez.server.util.GravityLogic;
import com.dragonminez.server.world.dimension.HTCDimension;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GravityLogic.class, remap = false)
public abstract class TrainingGravityDimensionMixin {
    @Inject(method = "getTrainingBonusGravity", at = @At("RETURN"), cancellable = true, remap = false)
    private static void dmzrevamp$includeTimeChamberAmbientGravityBonus(
            Player player,
            CallbackInfoReturnable<Double> cir
    ) {
        if (player.level().dimension().equals(HTCDimension.HTC_KEY)) {
            // HTC has its own TP multiplier, but its environmental gravity must still
            // contribute independently to the gravity TP bonus. getBonusGravity uses
            // the full dimension gravity, resistance and configured hard-stop threshold.
            cir.setReturnValue(GravityLogic.getBonusGravity(player));
        }
    }
}
