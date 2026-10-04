package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.quest.QuestSpawnAttributeApplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "com.dragonminez.server.events.QuestFieldSessions", remap = false)
public abstract class QuestFieldSessionsDefenseMixin {
    @Redirect(method = {"spawnWave", "spawnEscort"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;m_7967_(Lnet/minecraft/world/entity/Entity;)Z", remap = true))
    private static boolean dmzrevamp$applyDefenseBeforeSpawn(ServerLevel level, Entity entity) {
        QuestSpawnAttributeApplier.markVerifiedQuestSpawn(entity);
        QuestSpawnAttributeApplier.applyFromQuestTags(entity);
        return level.addFreshEntity(entity);
    }
}
