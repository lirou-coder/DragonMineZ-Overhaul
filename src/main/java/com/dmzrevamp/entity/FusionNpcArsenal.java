package com.dmzrevamp.entity;

import com.dragonminez.common.init.entities.sagas.DBSagasEntity;

public final class FusionNpcArsenal {
    public static final int FINAL_KAME_CORE = 0x00FFFF;
    public static final int FINAL_KAME_OUTER = 0xFFFF00;
    public static final int FINAL_KAME_OUTLINE = 0xFFFFFF;
    public static final int BIG_BANG_KAME_CORE = 0x004C99;
    public static final int BIG_BANG_KAME_OUTER = 0x001A66;
    public static final int BIG_BANG_KAME_OUTLINE = 0x66BFFF;
    private FusionNpcArsenal() {}

    public static void configureGogeta(DBSagasEntity entity) {
        entity.setAllowedCombos(120,
                DBSagasEntity.ComboType.AIR,
                DBSagasEntity.ComboType.KI_CHARGE_ATTACK,
                DBSagasEntity.ComboType.METEOR_COMBINATION,
                DBSagasEntity.ComboType.BASIC);
        entity.getSkillPool().clear();
        entity.addKiSkill(DBSagasEntity.KiSkillType.KI_VOLLEY, 200, 1.6F, 0xFFE657, 0xFFE657);
        entity.addKiSkill(DBSagasEntity.KiSkillType.BIG_BANG, 260, 1.9F, 0xE3FFFF, 0xE3FFFF);
        entity.addKiSkill(DBSagasEntity.KiSkillType.KAMEHAMEHA, 320, 2.6F,
                0xFFFFFF, 0x40C4FF, 0x0D47A1);
        entity.addKiSkill(DBSagasEntity.KiSkillType.SOUL_PUNISHER, 400, 3.5F);
    }
}
