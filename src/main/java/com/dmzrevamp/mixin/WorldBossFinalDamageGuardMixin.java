package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.combat.MobDefenseEvents;
import com.dragonminez.common.init.entities.worldboss.WorldBossEntity;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.Shadow;

/** Temporary compatibility guard: lets DMZ evaluate lethal damage after mob-defense mitigation. */
@Mixin(value = DBSagasEntity.class, remap = false)
public abstract class WorldBossFinalDamageGuardMixin {
    @Shadow protected abstract boolean canTransform();
    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private float dmzrevamp$useFinalBossDamage(float amount) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!(entity instanceof WorldBossEntity) || entity.level().isClientSide) return amount;
        float finalDamage = MobDefenseEvents.mitigatedDamage(entity, amount);
        if (canTransform() && finalDamage >= entity.getHealth()) {
            double trigger = entity.getPersistentData().contains("dmz_quest_tf_trigger")
                    ? entity.getPersistentData().getDouble("dmz_quest_tf_trigger")
                    : com.dragonminez.common.config.ConfigManager.getEntityTransformDefaults().triggerHealthFractionOr(0.5D);
            trigger = Math.max(0D, Math.min(1D, trigger));
            float floor = (float) (entity.getMaxHealth() * trigger * 0.5D);
            if (entity.getHealth() > floor) finalDamage = Math.min(finalDamage, entity.getHealth() - floor);
            else finalDamage = Math.min(finalDamage, Math.max(0.0001F, entity.getHealth() - 0.0001F));
        }
        if (finalDamage < amount) {
            MobDefenseEvents.markPreMitigatedBossDamage(entity);
            return finalDamage;
        }
        return amount;
    }

    @Inject(method = "hurt", at = @At("RETURN"), require = 0)
    private void dmzrevamp$clearDamageMarker(net.minecraft.world.damagesource.DamageSource source,
                                               float amount, CallbackInfoReturnable<Boolean> cir) {
        MobDefenseEvents.clearPreMitigatedBossDamage();
    }
}
