package com.dmzrevamp.mixin.client;

import com.dmzrevamp.revamp.combat.MobDefenseEvents;
import com.dragonminez.client.systems.kisense.CombatIndicators;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keeps Ki Sense damage popups aligned with damage mitigated by mob defense. */
@Mixin(value = CombatIndicators.class, remap = false)
public abstract class CombatIndicatorsMobDefenseMixin {
    @Unique private static final ThreadLocal<LivingEntity> DMZREVAMP$TRACKED_ENTITY = new ThreadLocal<>();

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;m_21223_()F",
                    remap = false
            )
    )
    private static float dmzrevamp$captureTrackedEntity(LivingEntity entity) {
        DMZREVAMP$TRACKED_ENTITY.set(entity);
        return entity.getHealth();
    }

    @ModifyArg(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dragonminez/client/systems/kisense/CombatIndicators;registerChange(Lcom/dragonminez/client/systems/kisense/CombatIndicators$Track;FZJ)V",
                    ordinal = 0
            ),
            index = 1
    )
    private static float dmzrevamp$showMitigatedMobDamage(float amount) {
        return MobDefenseEvents.mitigatedDamage(DMZREVAMP$TRACKED_ENTITY.get(), amount);
    }
}
