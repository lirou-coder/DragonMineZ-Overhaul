package com.dmzrevamp.mixin;

import com.dmzrevamp.entity.DmzRevampAttributes;
import com.dmzrevamp.revamp.quest.ConfiguredTransformDefenseData;
import com.dmzrevamp.revamp.quest.QuestSpawnAttributeApplier;
import com.dragonminez.common.config.RaidDefinition;
import com.dragonminez.common.quest.Difficulty;
import com.dragonminez.server.world.raid.Raid;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Raid.class, remap = false)
public abstract class RaidDefenseMixin {
    @Shadow @Final private Difficulty difficulty;

    @Inject(method = "applyStats", at = @At("RETURN"))
    private void dmzrevamp$applyDefense(Mob mob, RaidDefinition.Spawn spawn, CallbackInfo ci) {
        if (!(spawn instanceof ConfiguredTransformDefenseData data) || data.dmzrevamp$getDefense() == null) return;
        AttributeInstance defense = mob.getAttribute(DmzRevampAttributes.MOB_DEFENSE.get());
        if (defense != null) {
            double value = Math.max(0D, data.dmzrevamp$getDefense()) * difficulty.damageMultiplier();
            mob.getPersistentData().putDouble(QuestSpawnAttributeApplier.DEFENSE_TAG, value);
            defense.setBaseValue(value);
        }
    }

    @Inject(method = "applySpawnOptions", at = @At("RETURN"))
    private void dmzrevamp$saveTransformDefense(Mob mob, RaidDefinition.Spawn spawn, CallbackInfo ci) {
        if (!(spawn instanceof ConfiguredTransformDefenseData data) || data.dmzrevamp$getTransformDefense() == null) return;
        mob.getPersistentData().putDouble(QuestSpawnAttributeApplier.TF_DEFENSE_TAG,
                Math.max(0D, data.dmzrevamp$getTransformDefense()) * difficulty.damageMultiplier());
    }
}
