package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.combat.AdaptiveDefenseFullNegationEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityFullNegationMixin {
    @Inject(method = "m_6677_", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzrevamp$suppressFullyNegatedHurtSound(DamageSource source, CallbackInfo ci) {
        if (AdaptiveDefenseFullNegationEvents.isMarkedFullyNegated((LivingEntity) (Object) this)) ci.cancel();
    }
}
