package com.dmzrevamp.mixin;

import com.dmzrevamp.entity.FusionNpcArsenal;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = {
        "com.dragonminez.common.init.entities.sagas.SagaVegetaEntity$SagaVegettoBaseEntity",
        "com.dragonminez.common.init.entities.sagas.SagaVegetaEntity$SagaVegettoSSJEntity"
}, remap = false)
public abstract class SagaVegettoArsenalMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void dmzrevamp$replaceFusionArsenal(EntityType<?> type, Level level, CallbackInfo ci) {
        DBSagasEntity entity = (DBSagasEntity) (Object) this;
        boolean ssj = entity.getClass().getSimpleName().contains("SSJ");
        FusionNpcArsenal.configureVegetto(entity, ssj);
    }
}
