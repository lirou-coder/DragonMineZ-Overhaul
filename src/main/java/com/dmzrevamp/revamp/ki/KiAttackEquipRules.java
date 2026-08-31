package com.dmzrevamp.revamp.ki;

import com.dmzrevamp.config.DmzRevampConfig;
import com.dmzrevamp.revamp.strike.RevampStrikeAttackData;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.techniques.StrikeAttackData;
import com.dragonminez.common.stats.techniques.TechniqueData;
import com.dragonminez.common.stats.techniques.Techniques;

import java.util.Map;

public final class KiAttackEquipRules {
    private KiAttackEquipRules() {
    }

    public static KiAttackCategory blockedCategory(Techniques techniques, int targetSlot, String newTechniqueId) {
        if (!DmzRevampConfig.ENABLE_KI_ATTACK_CATEGORY_EQUIP_LIMITS.get()) {
            return null;
        }
        TechniqueData newTechnique = techniques.getUnlockedTechniques().get(newTechniqueId);
        KiAttackCategory newCategory = categoryOf(newTechnique);
        if (newCategory == null) {
            return null;
        }
        if (newCategory == KiAttackCategory.BASIC) {
            return null;
        }

        int count = countAfterEquip(techniques, targetSlot, newTechniqueId, newCategory);
        int limit = newCategory == KiAttackCategory.ADVANCED
                ? DmzRevampConfig.MAX_EQUIPPED_ADVANCED_KI_ATTACKS.get()
                : DmzRevampConfig.MAX_EQUIPPED_ULTIMATE_KI_ATTACKS.get();
        return count > limit ? newCategory : null;
    }

    private static int countAfterEquip(Techniques techniques, int targetSlot, String newTechniqueId, KiAttackCategory category) {
        String[] slots = techniques.getEquippedSlots();
        Map<String, TechniqueData> unlocked = techniques.getUnlockedTechniques();
        int count = 0;
        for (int i = 0; i < slots.length; i++) {
            String id = i == targetSlot ? newTechniqueId : slots[i];
            TechniqueData data = unlocked.get(id);
            if (categoryOf(data) == category) {
                count++;
            }
        }
        return count;
    }

    public static KiAttackCategory categoryOf(TechniqueData data) {
        if (data instanceof KiAttackData ki) {
            return categoryOf(ki);
        }
        if (data instanceof StrikeAttackData strike && strike instanceof RevampStrikeAttackData revamp) {
            return revamp.dmzrevamp$getCategory();
        }
        return null;
    }

    public static KiAttackCategory categoryOf(KiAttackData data) {
        if (data instanceof RevampKiAttackData revamp) {
            return revamp.dmzrevamp$getCategory();
        }
        return KiAttackCategoryRules.classify(data);
    }
}
