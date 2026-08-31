package com.dmzrevamp.mixin.client;

import com.dmzrevamp.revamp.cosmetic.InternalCosmeticArmorRows;
import com.dragonminez.client.render.layer.DMZPlayerArmorLayer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import software.bernie.geckolib.cache.object.GeoBone;

@Mixin(DMZPlayerArmorLayer.class)
public abstract class DMZPlayerArmorLayerInternalRowsMixin {
    @Inject(method = "getArmorItemForBone(Lsoftware/bernie/geckolib/cache/object/GeoBone;Lnet/minecraft/client/player/AbstractClientPlayer;)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzrevamp$useInternalArmorRow(GeoBone bone, AbstractClientPlayer player, CallbackInfoReturnable<ItemStack> cir) {
        EquipmentSlot slot = equipmentSlotForBone(bone);
        if (slot == null) {
            return;
        }

        ItemStack override = InternalCosmeticArmorRows.getClientOverride(player, slot);
        if (override != null) {
            cir.setReturnValue(override);
        }
    }

    private static EquipmentSlot equipmentSlotForBone(GeoBone bone) {
        if (bone == null) {
            return null;
        }
        return switch (bone.getName()) {
            case "armorHead", "armor_head" -> EquipmentSlot.HEAD;
            case "armorBody", "armor_body", "armorRightArm", "armor_right_arm", "armorLeftArm", "armor_left_arm" -> EquipmentSlot.CHEST;
            case "armorLeggingsBody", "armor_leggings_body", "armorLeftLeg", "armor_left_leg", "armorRightLeg", "armor_right_leg" -> EquipmentSlot.LEGS;
            case "armorRightBoot", "armor_right_boot", "armorLeftBoot", "armor_left_boot" -> EquipmentSlot.FEET;
            default -> null;
        };
    }
}
