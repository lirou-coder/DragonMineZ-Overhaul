package com.dmzrevamp.revamp.battlepower;

import com.dmzrevamp.revamp.quest.QuestMobEffectConfig;
import com.dmzrevamp.revamp.quest.RevampKillObjectiveData;
import com.dragonminez.common.quest.objectives.KillObjective;

import java.util.List;

/** Reads the objective values received from the server's live quest registry. */
public final class QuestPreviewExtraStatsResolver {
    private QuestPreviewExtraStatsResolver() {
    }

    static ExtraStats resolve(KillObjective objective) {
        ExtraStats stats = new ExtraStats();
        if (objective instanceof RevampKillObjectiveData data) {
            stats.defense = data.dmzrevamp$getDefense();
            stats.armor = data.dmzrevamp$getArmor();
            stats.armorToughness = data.dmzrevamp$getArmorToughness();
            stats.protection = data.dmzrevamp$getProtection();
            stats.movementSpeed = data.dmzrevamp$getMovementSpeed();
            stats.mobEffects = data.dmzrevamp$getMobEffects();
        }
        return stats;
    }

    public static final class ExtraStats {
        public Double defense;
        public Double armor;
        public Double armorToughness;
        public Double protection;
        public Double movementSpeed;
        public List<QuestMobEffectConfig> mobEffects = List.of();

        boolean hasAnyConfiguredValue() {
            return defense != null
                    || armor != null
                    || armorToughness != null
                    || protection != null
                    || movementSpeed != null
                    || (mobEffects != null && !mobEffects.isEmpty());
        }
    }
}
