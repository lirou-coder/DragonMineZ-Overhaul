package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.quest.QuestSpawnAttributeApplier;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "daripher.autoleveling.event.MobsLevelingEvents", remap = false)
public abstract class AutoLevelingQuestSpawnSkipMixin {
    @Inject(method = "shouldSetLevel", at = @At("HEAD"), cancellable = true)
    private static void dmzrevamp$skipDmzQuestSpawnedEntities(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (QuestSpawnAttributeApplier.isVerifiedQuestSpawn(entity)) {
            cir.setReturnValue(false);
        }
    }
}
