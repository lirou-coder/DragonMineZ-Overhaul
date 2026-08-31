package com.dmzrevamp.mixin;

import com.dmzrevamp.item.HandwearItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Player.class)
public abstract class PlayerHeldHandwearAttackMixin {
    @Redirect(
            method = "m_5706_(Lnet/minecraft/world/entity/Entity;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;m_21205_()Lnet/minecraft/world/item/ItemStack;",
                    remap = false
            ),
            remap = false,
            require = 0
    )
    private ItemStack dmzrevamp$hideHeldHandwearFromVanillaAttack(Player player) {
        ItemStack stack = player.getMainHandItem();
        // Handwear is powered through the Curios hands slot, not by holding the item as a weapon.
        return stack.getItem() instanceof HandwearItem ? ItemStack.EMPTY : stack;
    }
}
