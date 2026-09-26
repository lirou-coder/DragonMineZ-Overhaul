package com.dmzrevamp.mixin;

import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DBSagasEntity.class)
public interface DBSagasEntityComboTargetAccessor {
    @Accessor(value = "comboTarget", remap = false)
    LivingEntity dmzrevamp$getComboTarget();
}
