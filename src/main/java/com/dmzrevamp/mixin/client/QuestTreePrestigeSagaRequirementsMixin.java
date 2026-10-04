package com.dmzrevamp.mixin.client;

import com.dragonminez.client.gui.character.QuestTreeScreen;
import com.dragonminez.common.quest.PlayerQuestData;
import com.dragonminez.common.quest.Quest;
import com.dragonminez.common.quest.QuestRegistry;
import com.dragonminez.common.quest.Saga;
import com.dragonminez.common.stats.StatsData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The DMZ manifest format supports one previous saga; Prestige requires both parallel branches. */
@Mixin(value = QuestTreeScreen.class, remap = false)
public abstract class QuestTreePrestigeSagaRequirementsMixin {
    @Shadow private StatsData statsData;

    @Inject(method = "isSagaUnlockedByPreviousCompletion", at = @At("RETURN"), cancellable = true)
    private void dmzrevamp$requireDaimaAndGt(Saga saga, CallbackInfoReturnable<Boolean> cir) {
        if (saga == null || !"prestige_saga".equalsIgnoreCase(saga.getId()) || statsData == null) return;
        PlayerQuestData quests = statsData.getPlayerQuestData();
        for (Quest quest : saga.getQuests()) {
            if (quests.getQuestStatus(PlayerQuestData.sagaQuestKey(saga.getId(), quest.getId()))
                    != PlayerQuestData.QuestStatus.NOT_STARTED) return;
        }
        cir.setReturnValue(dmzrevamp$isCompleted("daima_saga", quests)
                && dmzrevamp$isCompleted("gt_saga", quests));
    }

    private static boolean dmzrevamp$isCompleted(String sagaId, PlayerQuestData data) {
        Saga saga = QuestRegistry.getClientSagas().get(sagaId);
        if (saga == null || saga.getQuests().isEmpty()) return false;
        for (Quest quest : saga.getQuests()) {
            if (!data.isQuestCompleted(PlayerQuestData.sagaQuestKey(sagaId, quest.getId()))) return false;
        }
        return true;
    }
}
