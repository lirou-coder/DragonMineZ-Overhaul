package com.dmzrevamp.mixin.client;

import com.dmzrevamp.revamp.cosmetic.InternalCosmeticArmorRows;
import com.dragonminez.client.render.layer.DMZCustomArmorLayer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DMZCustomArmorLayer.class)
public abstract class DMZCustomArmorLayerInternalRowsMixin {
    @Inject(method = "resolveChestArmorStack", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzrevamp$useInternalChestRow(AbstractClientPlayer player, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack override = InternalCosmeticArmorRows.getClientOverride(player, EquipmentSlot.CHEST);
        if (override != null) {
            cir.setReturnValue(override);
        }
    }
}
