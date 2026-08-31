package com.dmzrevamp.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.Optional;

public final class HandwearHelper {
    public static final String HANDS_SLOT = "hands";

    private HandwearHelper() {
    }

    public static boolean canUseHandwear(LivingEntity entity) {
        // DMZ's blade, scythe and dual-blade Ki Weapons are virtual weapons:
        // they are active specifically while the real main-hand stack is empty.
        return entity != null && entity.getMainHandItem().isEmpty();
    }

    public static Optional<ItemStack> getActiveHandwear(LivingEntity entity, HandwearType type) {
        if (!canUseHandwear(entity)) {
            return Optional.empty();
        }
        return getEquippedHandwear(entity, type);
    }

    public static Optional<ItemStack> getEquippedHandwear(LivingEntity entity, HandwearType type) {
        if (entity == null) {
            return Optional.empty();
        }
        return CuriosApi.getCuriosInventory(entity)
                .resolve()
                .stream()
                .flatMap(handler -> handler.findCurios(stack -> stack.getItem() instanceof HandwearItem handwear && handwear.getHandwearType() == type).stream())
                .filter(result -> HANDS_SLOT.equals(result.slotContext().identifier()))
                .map(SlotResult::stack)
                .findFirst();
    }

    public static Optional<ItemStack> getAnyActiveHandwear(LivingEntity entity) {
        if (!canUseHandwear(entity)) {
            return Optional.empty();
        }
        return CuriosApi.getCuriosInventory(entity)
                .resolve()
                .stream()
                .flatMap(handler -> handler.findCurios(stack -> stack.getItem() instanceof HandwearItem).stream())
                .filter(result -> HANDS_SLOT.equals(result.slotContext().identifier()))
                .map(SlotResult::stack)
                .findFirst();
    }

    /** Weapon stack seen by systems that normally only inspect the main hand. */
    public static ItemStack effectiveMainHand(LivingEntity entity) {
        if (entity == null) return ItemStack.EMPTY;
        ItemStack held = entity.getMainHandItem();
        return held.isEmpty() ? getAnyActiveHandwear(entity).orElse(ItemStack.EMPTY) : held;
    }

    public static boolean hasActiveGloves(LivingEntity entity) {
        return getActiveHandwear(entity, HandwearType.GLOVES).isPresent();
    }

    public static boolean hasActiveWristbands(LivingEntity entity) {
        return getActiveHandwear(entity, HandwearType.WRISTBANDS).isPresent();
    }

    public static boolean hasEquippedGloves(LivingEntity entity) {
        return getEquippedHandwear(entity, HandwearType.GLOVES).isPresent();
    }

    public static boolean hasEquippedWristbands(LivingEntity entity) {
        return getEquippedHandwear(entity, HandwearType.WRISTBANDS).isPresent();
    }

}
