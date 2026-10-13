package com.dmzrevamp.entity;

import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.dragonminez.common.init.MainEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public final class SagaGogetaEntity {
    private SagaGogetaEntity() {}

    private static void configure(DBSagasEntity entity) {
        entity.setCanFly(true);
        entity.setAuraColor(0xFFFFFF);
        entity.setKiBlastSpeed(2.2F);
        entity.setDBZStyle(0);
        entity.setEvade(true, 60);
        entity.setWildSense(true, 100);
        FusionNpcArsenal.configureGogeta(entity);
    }

    public static class Base extends DBSagasEntity {
        public Base(EntityType<? extends Monster> type, Level level) {
            super(type, level);
            configure(this);
        }

        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return MainEntities.SAGA_GOGETA_SSJ.get();
        }

        @Override
        protected boolean spawnsNewFormFullHealth() {
            return true;
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_gogeta_base";
        }
    }

}
