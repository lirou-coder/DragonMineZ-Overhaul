package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.quest.QuestSpawnAttributeApplier;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DBSagasEntity.class)
public abstract class DBSagasEntityRevampTransformFieldsMixin {
    @Inject(method = "finishTransformationSpawn", at = @At("HEAD"), remap = false)
    private void dmzrevamp$copyRevampQuestTagsBeforeSpawn(DBSagasEntity transformed, boolean fullHealth, CallbackInfo ci) {
        QuestSpawnAttributeApplier.copyRevampQuestTags((DBSagasEntity) (Object) this, transformed);
    }

    @Inject(method = "finishTransformationSpawn", at = @At("RETURN"), remap = false)
    private void dmzrevamp$applyRevampQuestTransformFields(DBSagasEntity transformed, boolean fullHealth, CallbackInfo ci) {
        QuestSpawnAttributeApplier.applyTransformAttributes((DBSagasEntity) (Object) this, transformed);
    }
}
