package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.combat.AdaptiveDefenseFullNegationEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = com.dragonminez.server.events.players.combat.CombatEvent.class, remap = false)
public abstract class CombatEventFullyNegatedHitMixin {
    @ModifyArg(
            method = "overrideVanillaArmorReduction",
            at = @At(value = "INVOKE", target = "Lnet/minecraftforge/eventbus/api/Event;setCanceled(Z)V"),
            index = 0,
            require = 0,
            remap = false
    )
    private static boolean dmzrevamp$keepNegatedDamageEventConnected(boolean canceled) {
        return false;
    }

    @ModifyArg(
            method = "overrideVanillaArmorReduction",
            at = @At(value = "INVOKE", target = "Lnet/minecraftforge/event/entity/living/LivingDamageEvent;setCanceled(Z)V"),
            index = 0,
            require = 0,
            remap = false
    )
    private static boolean dmzrevamp$keepNegatedLivingDamageConnected(boolean canceled) {
        return false;
    }

    @Inject(method = "applyFullNegation", at = @At("HEAD"), require = 0, remap = false)
    private static void dmzrevamp$markFullyNegatedHit(LivingEntity target, CallbackInfo ci) {
        AdaptiveDefenseFullNegationEvents.markFullyNegated(target);
    }
}
