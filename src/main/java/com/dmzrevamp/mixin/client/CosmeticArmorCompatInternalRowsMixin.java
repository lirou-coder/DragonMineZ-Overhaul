package com.dmzrevamp.mixin.client;

import com.dmzrevamp.revamp.cosmetic.InternalCosmeticArmorRows;
import com.dragonminez.client.render.compat.CosmeticArmorCompat;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CosmeticArmorCompat.class)
public abstract class CosmeticArmorCompatInternalRowsMixin {
    @Inject(method = "getCosmeticStack", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dmzrevamp$useInternalCosmeticRows(Player player, EquipmentSlot slot, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack override = InternalCosmeticArmorRows.getClientOverride(player, slot);
        if (override != null) {
            cir.setReturnValue(override);
        }
    }
}
