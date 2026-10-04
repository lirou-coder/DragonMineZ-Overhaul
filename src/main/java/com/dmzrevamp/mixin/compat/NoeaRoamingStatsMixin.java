package com.dmzrevamp.mixin.compat;

import com.dmzrevamp.compat.NoeaNpcStatCompat;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.butterjaffa.noeabosses.StoryRoamingService", remap = false)
public abstract class NoeaRoamingStatsMixin {
    @Inject(method = "spawn", at = @At("RETURN"), require = 0)
    private static void dmzrevamp$includeDefenseInRoamingDistribution(
            ServerPlayer player, @Coerce Object moment, boolean forced,
            CallbackInfoReturnable<DBSagasEntity> cir) {
        DBSagasEntity entity = cir.getReturnValue();
        if (entity != null) NoeaNpcStatCompat.applyTarget(entity, moment, 1D);
    }
}
