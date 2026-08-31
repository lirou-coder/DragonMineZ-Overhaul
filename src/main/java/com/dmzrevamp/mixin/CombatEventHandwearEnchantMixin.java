package com.dmzrevamp.mixin;

import com.dmzrevamp.item.HandwearHelper;
import com.dragonminez.server.events.players.combat.CombatEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = CombatEvent.class, remap = false)
public abstract class CombatEventHandwearEnchantMixin {
    @Redirect(method = "computeDefensePenetration", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;m_21205_()Lnet/minecraft/world/item/ItemStack;"), require = 0)
    private static ItemStack dmzrevamp$useCurioHandwearEnchantments(LivingEntity entity) {
        return HandwearHelper.effectiveMainHand(entity);
    }
}
