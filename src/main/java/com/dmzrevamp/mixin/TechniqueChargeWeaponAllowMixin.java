package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.combat.HeldItemTechniqueUse;
import com.dragonminez.common.network.C2S.TechniqueChargeC2S;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TechniqueChargeC2S.class)
public abstract class TechniqueChargeWeaponAllowMixin {
    @Redirect(
            method = "lambda$handle$0",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;m_41619_()Z"),
            remap = false
    )
    private static boolean dmzrevamp$allowKiTechniquesWithDmzWeapons(ItemStack stack) {
        return HeldItemTechniqueUse.isEmptyOrDmzWeapon(stack);
    }
}
