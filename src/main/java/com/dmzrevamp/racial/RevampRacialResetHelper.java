package com.dmzrevamp.racial;

import com.dmzrevamp.racial.impl.MajinRevampRacialSkill;
import com.dmzrevamp.racial.impl.NamekianRevampRacialSkill;
import com.dmzrevamp.racial.impl.SaiyanRpgZenkaiEvents;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.racial.RacialData;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.UUID;

/** Performs the complete racial-state reset shared by every DMZ reset entry point. */
public final class RevampRacialResetHelper {
    private RevampRacialResetHelper() {
    }

    public static void resetAll(ServerPlayer player, StatsData data) {
        SaiyanRpgZenkaiEvents.resetZenkai(player, data);
        MajinRevampRacialSkill.resetAbsorption(player, data);
        NamekianRevampRacialSkill.resetAssimilation(player, data);

        RacialData racialData = data.getRacialData();
        int barrierEntityId = racialData.getAndroidBarrierEntityId();
        if (barrierEntityId >= 0) {
            Entity barrier = player.level().getEntity(barrierEntityId);
            if (barrier != null) barrier.discard();
        }
        for (String bonusName : new ArrayList<>(racialData.getOwnedBonusNames())) {
            data.getBonusStats().removeAllBonuses(bonusName);
        }

        // Cell Juniors are persistent entities owned by the racial. A complete reset must
        // remove them instead of only forgetting their UUIDs and leaving orphaned mobs.
        for (UUID id : new ArrayList<>(racialData.getCellJrs())) {
            for (ServerLevel level : player.server.getAllLevels()) {
                Entity entity = level.getEntity(id);
                if (entity != null) {
                    entity.discard();
                    break;
                }
            }
        }

        // Loading an empty tag restores every persisted native racial field to its default,
        // including eject slots, use counters, reserve, adrenaline and Bio-Android state.
        racialData.load(new net.minecraft.nbt.CompoundTag());
        racialData.setAndroidBarrierActive(false);
        racialData.setAndroidBarrierTicks(0);
        racialData.setAndroidBarrierEntityId(-1);
        racialData.setBioBlastCenter(null);
        racialData.setBioBlastMaxRadius(0F);
        racialData.setBioBlastTick(0);
        racialData.setPendingCaptureRequest(null);

        data.getResources().setRacialSkillCount(0);
        CustomRacialCooldownEvents.clearAllRacialCooldowns(player);
        NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
    }
}
