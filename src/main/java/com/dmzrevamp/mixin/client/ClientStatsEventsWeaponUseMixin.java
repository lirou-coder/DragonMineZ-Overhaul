package com.dmzrevamp.mixin.client;

import com.dmzrevamp.revamp.combat.HeldItemTechniqueUse;
import com.dragonminez.client.events.ClientStatsEvents;
import com.dragonminez.common.combat.logic.player.PlayerAttackHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientStatsEvents.class)
public abstract class ClientStatsEventsWeaponUseMixin {
    @Redirect(
            method = "canActivateTechnique",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;m_41619_()Z"),
            remap = false
    )
    private static boolean dmzrevamp$allowTechniqueActivationWithDmzWeapons(ItemStack stack) {
        return HeldItemTechniqueUse.isEmptyOrDmzWeapon(stack);
    }

    @Redirect(
            method = "lambda$onClientTick$3",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;m_41619_()Z"),
            remap = false
    )
    private static boolean dmzrevamp$treatDmzWeaponsAsEmptyForBlocking(ItemStack stack) {
        return HeldItemTechniqueUse.canBlockWithHeldItem(stack);
    }

    @Redirect(
            method = "lambda$onClientTick$3",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/combat/logic/player/PlayerAttackHelper;isKiWeaponActive(Lnet/minecraft/world/entity/player/Player;)Z"),
            remap = false
    )
    private static boolean dmzrevamp$allowBlockingWithActiveKiWeapon(Player player) {
        // These checks only gate guarding. The Ki Weapon remains active for
        // combat, rendering and combo resolution everywhere else.
        return false;
    }
}
