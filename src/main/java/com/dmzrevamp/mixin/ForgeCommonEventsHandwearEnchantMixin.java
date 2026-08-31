package com.dmzrevamp.mixin;

import com.dmzrevamp.item.HandwearHelper;
import com.dragonminez.common.events.ForgeCommonEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ForgeCommonEvents.class, remap = false)
public abstract class ForgeCommonEventsHandwearEnchantMixin {
    @Redirect(method = {"getCriticalChance", "getCriticalDamage"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;m_21205_()Lnet/minecraft/world/item/ItemStack;"), require = 0)
    private static ItemStack dmzrevamp$useCurioHandwearEnchantments(Player entity) {
        return HandwearHelper.effectiveMainHand(entity);
    }
}
