package com.dmzrevamp.mixin;

import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = "com.dragonminez.common.init.entities.sagas.SagaVegetaEntity$SagaVegettoBaseEntity", remap = false)
public abstract class SagaVegettoBaseTransformMixin {
    protected boolean hasTransformation() {
        return true;
    }

    public EntityType<? extends DBSagasEntity> getNextTransform() {
        return MainEntities.SAGA_VEGETTO_SSJ.get();
    }
}
