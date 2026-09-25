package com.dmzrevamp.mixin;

import com.dmzrevamp.item.HandwearItem;
import com.dragonminez.common.combat.logic.player.PlayerAttackHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Handwear is a SwordItem only so DMZ/vanilla weapon enchantments can be put on
 * it. Its actual gameplay effects come only from a HandwearItem equipped in
 * the Curios hands slot while the player's REAL main hand is empty. This mixin
 * does not activate Curios handwear or expose its enchantments/stat bonuses.
 *
 * DMZ 2.2 redirects Player#attack#getMainHandItem itself, so redirecting the
 * same invocation from another Player mixin conflicts with DMZ's PlayerMixin.
 * Instead, only the melee attack-selection helpers treat a physically held
 * HandwearItem as non-weapon/empty so its SwordItem base class cannot select a
 * sword combo. isKiWeaponActive is deliberately NOT redirected: a real item
 * in the main hand must continue to block Ki Weapons and Curios handwear.
 */
@Mixin(value = PlayerAttackHelper.class, remap = false)
public abstract class PlayerHeldHandwearAttackMixin {
    @Redirect(
            method = {"getCurrentAttack", "isDualWielding", "isTwoHandedWielding"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;m_21205_()Lnet/minecraft/world/item/ItemStack;"
            ),
            remap = false,
            require = 0
    )
    private static ItemStack dmzrevamp$treatHeldHandwearAsEmpty(Player player) {
        ItemStack stack = player.getMainHandItem();
        return stack.getItem() instanceof HandwearItem ? ItemStack.EMPTY : stack;
    }
}
