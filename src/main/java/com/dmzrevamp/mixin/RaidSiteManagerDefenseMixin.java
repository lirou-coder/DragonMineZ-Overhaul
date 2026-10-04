package com.dmzrevamp.mixin;

import com.dmzrevamp.entity.DmzRevampAttributes;
import com.dmzrevamp.revamp.quest.ConfiguredDefenseData;
import com.dmzrevamp.revamp.quest.QuestSpawnAttributeApplier;
import com.dragonminez.common.config.RaidDefinition;
import com.dragonminez.server.world.raid.RaidSiteManager;
import com.dragonminez.server.world.raid.RaidType;
import com.dragonminez.server.world.raid.RaidTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RaidSiteManager.class, remap = false)
public abstract class RaidSiteManagerDefenseMixin {
    @Inject(method = "createScout", at = @At("RETURN"))
    private static void dmzrevamp$applyScoutDefense(ServerLevelAccessor level, RaidSiteManager.Kind kind,
                                                     long siteKey, BlockPos stand, BlockPos home,
                                                     CallbackInfoReturnable<Mob> cir) {
        Mob scout = cir.getReturnValue();
        RaidType type = RaidTypes.get(((RaidSiteKindAccessor) (Object) kind).dmzrevamp$getRaidId());
        RaidDefinition.Trigger trigger = type != null && type.hasTrigger() ? type.getTrigger() : null;
        if (scout == null || !(trigger instanceof ConfiguredDefenseData data) || data.dmzrevamp$getDefense() == null) return;
        scout.getPersistentData().putDouble(QuestSpawnAttributeApplier.DEFENSE_TAG,
                Math.max(0D, data.dmzrevamp$getDefense()));
        AttributeInstance defense = scout.getAttribute(DmzRevampAttributes.MOB_DEFENSE.get());
        if (defense != null) defense.setBaseValue(Math.max(0D, data.dmzrevamp$getDefense()));
    }
}
