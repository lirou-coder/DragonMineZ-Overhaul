package com.dmzrevamp.mixin;

import com.dmzrevamp.item.HandwearItem;
import com.dragonminez.common.init.MainEnchants;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * DragonMineZ's weapon enchantments override Enchantment#canEnchant and only
 * accept SwordItem, AxeItem or DMZ WeaponItem instances. Handwear deliberately
 * remains a plain Item, so let only these DMZ weapon enchantments recognize it
 * as a valid weapon enchantment target without changing any other item behavior.
 */
@Mixin(value = {
        MainEnchants.WeaponPenetrationEnchantment.class,
        MainEnchants.HealingReductionEnchantment.class,
        MainEnchants.CriticalStatEnchantment.class
}, remap = false)
public abstract class DmzWeaponEnchantmentHandwearMixin {
    @Inject(
            method = {"canEnchant", "m_44689_"},
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private void dmzrevamp$allowHandwearForDmzWeaponEnchantments(ItemStack stack,
                                                                  CallbackInfoReturnable<Boolean> cir) {
        if (stack.getItem() instanceof HandwearItem) {
            cir.setReturnValue(true);
        }
    }
}
