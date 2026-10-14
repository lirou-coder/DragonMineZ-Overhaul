package com.dmzrevamp.revamp.quest;

import com.dmzrevamp.DmzRevampMod;
import com.dragonminez.common.quest.PartyManager;
import com.dragonminez.common.quest.Quest;
import com.dragonminez.common.quest.QuestRegistry;
import com.dragonminez.common.quest.QuestService;
import com.dragonminez.common.quest.objectives.SparObjective;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SparQuestProtectionEvents {
    private SparQuestProtectionEvents() {
    }

    /** Spar partners can still hit and apply hit effects, but cannot kill their quest participants. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void preventLethalSparDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide) {
            return;
        }

        DBSagasEntity spar = findSparAttacker(event.getSource().getEntity(), event.getSource().getDirectEntity());
        if (spar == null || !isOwnedSparPartner(spar, player)) {
            return;
        }

        float nonLethalHeadroom = Math.max(0.0F, player.getHealth() - 1.0F);
        if (event.getAmount() > nonLethalHeadroom) {
            event.setAmount(nonLethalHeadroom);
        }
    }

    private static DBSagasEntity findSparAttacker(Entity source, Entity direct) {
        if (source instanceof DBSagasEntity saga) {
            return saga;
        }
        if (direct instanceof DBSagasEntity saga) {
            return saga;
        }
        if (direct instanceof Projectile projectile && projectile.getOwner() instanceof DBSagasEntity saga) {
            return saga;
        }
        return null;
    }

    private static boolean isOwnedSparPartner(DBSagasEntity entity, ServerPlayer player) {
        var tag = entity.getPersistentData();
        if (!tag.contains(QuestService.QUEST_KEY_TAG)
                || !tag.contains(QuestService.QUEST_OBJECTIVE_INDEX_TAG)
                || !tag.contains(QuestService.QUEST_OWNER_TAG)) {
            return false;
        }

        Quest quest = QuestRegistry.getQuest(tag.getString(QuestService.QUEST_KEY_TAG));
        int objectiveIndex = tag.getInt(QuestService.QUEST_OBJECTIVE_INDEX_TAG);
        if (quest == null || objectiveIndex < 0 || objectiveIndex >= quest.getObjectives().size()
                || !(quest.getObjectives().get(objectiveIndex) instanceof SparObjective)) {
            return false;
        }

        String owner = tag.getString(QuestService.QUEST_OWNER_TAG);
        List<ServerPlayer> party = PartyManager.getAllPartyMembers(player);
        return party.stream().anyMatch(member -> member.getStringUUID().equals(owner));
    }
}
