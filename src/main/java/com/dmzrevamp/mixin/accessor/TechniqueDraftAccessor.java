package com.dmzrevamp.mixin.accessor;

import com.dragonminez.client.gui.character.TechniqueDraft;
import com.dragonminez.common.stats.techniques.KiAttackData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TechniqueDraft.class)
public interface TechniqueDraftAccessor {
    @Accessor("type") void dmzrevamp$setType(KiAttackData.KiType value);
    @Accessor("utility") void dmzrevamp$setUtility(KiAttackData.Utility value);
    @Accessor("damage") void dmzrevamp$setDamage(float value);
    @Accessor("size") void dmzrevamp$setSize(float value);
    @Accessor("speed") void dmzrevamp$setSpeed(float value);
    @Accessor("armorPen") void dmzrevamp$setArmorPen(int value);
    @Accessor("cooldown") void dmzrevamp$setCooldown(int value);
    @Accessor("kiCost") void dmzrevamp$setKiCost(float value);
    @Accessor("tpCost") void dmzrevamp$setTpCost(float value);
    @Accessor("secondaryType") void dmzrevamp$setSecondaryType(KiAttackData.SecondaryEffectType value);
    @Accessor("affectedStat") void dmzrevamp$setAffectedStat(KiAttackData.AffectedStat value);
    @Accessor("secondaryIntensity") void dmzrevamp$setSecondaryIntensity(int value);
    @Accessor("secondaryDuration") void dmzrevamp$setSecondaryDuration(int value);
}
