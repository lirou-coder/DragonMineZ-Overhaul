package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.quest.QuestSpawnAttributeApplier;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = DBSagasEntity.class, remap = false)
public abstract class DBSagasEntityTransformEntityOverrideMixin {
    @Shadow private boolean transformationDisabled;

    @Inject(method = "canTransform", at = @At("RETURN"), cancellable = true)
    private void dmzrevamp$allowConfiguredTransformation(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() || transformationDisabled) return;
        DBSagasEntity self = (DBSagasEntity) (Object) this;
        if (self.getPersistentData().getBoolean(QuestSpawnAttributeApplier.QUEST_NO_TRANSFORM_TAG)) return;
        cir.setReturnValue(dmzrevamp$configuredType(self) != null);
    }

    @Redirect(
            method = {"tick", "m_8119_"},
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/init/entities/sagas/DBSagasEntity;getNextTransform()Lnet/minecraft/world/entity/EntityType;")
    )
    private EntityType<? extends DBSagasEntity> dmzrevamp$overrideNextEntity(DBSagasEntity entity) {
        EntityType<? extends DBSagasEntity> configured = dmzrevamp$configuredType(entity);
        return configured != null ? configured : entity.getNextTransform();
    }

    @SuppressWarnings("unchecked")
    private static EntityType<? extends DBSagasEntity> dmzrevamp$configuredType(DBSagasEntity entity) {
        int stage = Math.max(1, Math.min(4,
                entity.getPersistentData().getInt(QuestSpawnAttributeApplier.TRANSFORM_STAGE_TAG) + 1));
        String rawId = entity.getPersistentData().getString(
                QuestSpawnAttributeApplier.TRANSFORM_ENTITY_TAG_PREFIX + stage);
        ResourceLocation id = ResourceLocation.tryParse(rawId);
        if (id == null) return null;
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(id);
        if (type == null) return null;
        Entity probe = type.create(entity.level());
        return probe instanceof DBSagasEntity ? (EntityType<? extends DBSagasEntity>) type : null;
    }
}
