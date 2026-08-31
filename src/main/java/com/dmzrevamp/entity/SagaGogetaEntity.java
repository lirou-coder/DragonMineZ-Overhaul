package com.dmzrevamp.entity;

import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public final class SagaGogetaEntity {
    private SagaGogetaEntity() {}

    private static void configure(DBSagasEntity entity, boolean ssj) {
        entity.setCanFly(true);
        entity.setAuraColor(ssj ? 0xFFF057 : 0xFFFFFF);
        entity.setKiBlastSpeed(1.6F);
        entity.setDBZStyle(0);
        if (ssj) {
            entity.setWildSense(true, 100);
            entity.setZanzoken(true, 200);
        } else {
            entity.setEvade(true, 60);
            entity.setWildSense(true, 100);
        }
        FusionNpcArsenal.configureGogeta(entity);
    }

    public static class Base extends DBSagasEntity {
        public Base(EntityType<? extends Monster> type, Level level) {
            super(type, level);
            configure(this, false);
        }

        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return DmzRevampEntities.SAGA_GOGETA_SSJ.get();
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

    public static class Ssj extends DBSagasEntity {
        public Ssj(EntityType<? extends Monster> type, Level level) {
            super(type, level);
            configure(this, true);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_gogeta_ssj";
        }
    }
}
