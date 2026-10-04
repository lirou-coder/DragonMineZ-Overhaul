package com.dmzrevamp.mixin;

import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.dragonminez.common.init.entities.sagas.SagaDaimaEntity;
import com.dragonminez.common.init.entities.sagas.SagaGTEntity;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;

/** Restores the missing final SSJ3/SSJ2 -> SSJ4 links in DMZ 2.2's saga entity chains. */
@Mixin(value = {
        SagaDaimaEntity.GokuMiniSSJ3Entity.class,
        SagaDaimaEntity.GokuDaimaSSJ3Entity.class,
        SagaGTEntity.GokuGTSSJ3Entity.class,
        SagaGTEntity.VegetaGTSSJ2Entity.class
}, remap = false)
public abstract class SagaSsj4TransformMixin {
    protected boolean hasTransformation() {
        return true;
    }

    public EntityType<? extends DBSagasEntity> getNextTransform() {
        Object entity = this;
        if (entity instanceof SagaDaimaEntity.GokuMiniSSJ3Entity) {
            return MainEntities.SAGA_GOKU_MINI_SSJ4.get();
        }
        if (entity instanceof SagaDaimaEntity.GokuDaimaSSJ3Entity) {
            return MainEntities.SAGA_GOKU_DAIMA_SSJ4.get();
        }
        if (entity instanceof SagaGTEntity.GokuGTSSJ3Entity) {
            return MainEntities.SAGA_GOKU_GT_SSJ4.get();
        }
        return MainEntities.SAGA_VEGETA_GT_SSJ4.get();
    }
}
