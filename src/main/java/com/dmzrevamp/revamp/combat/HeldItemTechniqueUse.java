package com.dmzrevamp.revamp.combat;

import com.dragonminez.common.combat.logic.weapon.WeaponRegistry;
import net.minecraft.world.item.ItemStack;
import com.dragonminez.common.init.item.weapons.BlasterCannonItem;
import com.dragonminez.common.init.item.weapons.MerusLaserItem;

public final class HeldItemTechniqueUse {
    private HeldItemTechniqueUse() {
    }

    public static boolean isEmptyOrDmzWeapon(ItemStack stack) {
        return stack == null || stack.isEmpty() || WeaponRegistry.getAttributes(stack) != null;
    }

    public static boolean canBlockWithHeldItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return true;
        if (WeaponRegistry.getAttributes(stack) == null
                && !(stack.getItem() instanceof BlasterCannonItem)
                && !(stack.getItem() instanceof MerusLaserItem)) return false;
        // Charged right-click weapons (tridents, bows, shields and similar items)
        // must keep their own use action instead of starting DMZ guarding too.
        return stack.getUseDuration() <= 0
                && stack.getUseAnimation() == net.minecraft.world.item.UseAnim.NONE;
    }
}
