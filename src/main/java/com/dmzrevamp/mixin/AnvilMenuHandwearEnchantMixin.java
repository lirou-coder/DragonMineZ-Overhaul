package com.dmzrevamp.mixin;

import com.dmzrevamp.item.HandwearItem;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Only replaces the anvil's enchantment compatibility query for Overhaul handwear.
 * The real stack and every other anvil operation remain untouched.
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuHandwearEnchantMixin {
    @Redirect(
            method = "createResult",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/enchantment/Enchantment;canEnchant(Lnet/minecraft/world/item/ItemStack;)Z"
            ),
            require = 0
    )
    private boolean dmzrevamp$allowSwordEnchantmentsOnHandwear(Enchantment enchantment, ItemStack stack) {
        if (stack.getItem() instanceof HandwearItem) {
            return HandwearItem.canAcceptSwordEnchantment(enchantment);
        }
        return enchantment.canEnchant(stack);
    }
}
