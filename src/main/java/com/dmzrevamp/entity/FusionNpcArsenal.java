package com.dmzrevamp.entity;

import com.dragonminez.common.init.entities.sagas.DBSagasEntity;

public final class FusionNpcArsenal {
    public static final int FINAL_KAME_CORE = 0x00FFFF;
    public static final int FINAL_KAME_OUTER = 0xFFFF00;
    public static final int FINAL_KAME_OUTLINE = 0xFFFFFF;
    public static final int BIG_BANG_KAME_CORE = 0x004C99;
    public static final int BIG_BANG_KAME_OUTER = 0x001A66;
    public static final int BIG_BANG_KAME_OUTLINE = 0x66BFFF;
    public static final int SOUL_PUNISHER_COLOR = 0xFFFFFF;

    private FusionNpcArsenal() {}

    public static void configureCombos(DBSagasEntity entity) {
        entity.setAllowedCombos(120,
                DBSagasEntity.ComboType.BASIC,
                DBSagasEntity.ComboType.AIR,
                DBSagasEntity.ComboType.KI_CHARGE_ATTACK,
                DBSagasEntity.ComboType.METEOR_COMBINATION);
    }

    public static void configureVegetto(DBSagasEntity entity, boolean ssj) {
        configureCombos(entity);
        entity.getSkillPool().clear();
        entity.addKiSkill(DBSagasEntity.KiSkillType.KI_VOLLEY, ssj ? 260 : 200, 1.2F,
                ssj ? 0xFFF057 : 0x00C0FF, ssj ? 0xFFF057 : 0x00C0FF);
        entity.addKiSkill(DBSagasEntity.KiSkillType.BIG_BANG, 340, 1.7F, 0xE3FFFF, 0xE3FFFF);
        entity.addKiSkill(DBSagasEntity.KiSkillType.GALICK_GUN, 400, 1.2F);
        entity.addKiSkill(DBSagasEntity.KiSkillType.KAMEHAMEHA, 300, 1.5F);
        entity.addKiSkill(DBSagasEntity.KiSkillType.GENERIC_KI_WAVE, 480, 1.0F,
                FINAL_KAME_CORE, FINAL_KAME_OUTER, FINAL_KAME_OUTLINE);
    }

    public static void configureGogeta(DBSagasEntity entity) {
        configureCombos(entity);
        entity.getSkillPool().clear();
        entity.addKiSkill(DBSagasEntity.KiSkillType.BIG_BANG, 340, 1.7F, 0xE3FFFF, 0xE3FFFF);
        entity.addKiSkill(DBSagasEntity.KiSkillType.KAMEHAMEHA, 300, 1.5F);
        // The NPC enum has no Soul Punisher entry. BIG_BANG supplies its native medium-ball projectile;
        // the spawn hook below identifies this white 5x variant and restores Soul Punisher's x3.5 damage.
        entity.addKiSkill(DBSagasEntity.KiSkillType.BIG_BANG, 460, 5.0F,
                SOUL_PUNISHER_COLOR, SOUL_PUNISHER_COLOR, SOUL_PUNISHER_COLOR);
        entity.addKiSkill(DBSagasEntity.KiSkillType.GENERIC_KI_WAVE, 480, 2.0F,
                BIG_BANG_KAME_CORE, BIG_BANG_KAME_OUTER, BIG_BANG_KAME_OUTLINE);
    }
}
