package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.ki.KiBlockProtection;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractKiProjectile.class)
public abstract class AbstractKiProjectileSupportBlockProtectionMixin {
    @Redirect(
            method = "destroyKiBlock",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_46961_(Lnet/minecraft/core/BlockPos;Z)Z"),
            remap = false
    )
    private boolean dmzrevamp$protectPlayerSupportBlocksFromDestroy(Level level, BlockPos pos, boolean drop) {
        if (dmzrevamp$isProtectedSupportBlock(pos)) {
            return true;
        }
        return level.destroyBlock(pos, drop);
    }

    @Redirect(
            method = "setKiBlockToAir",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_7731_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"),
            remap = false
    )
    private boolean dmzrevamp$protectPlayerSupportBlocksFromSetAir(Level level, BlockPos pos, BlockState state, int flags) {
        if (dmzrevamp$isProtectedSupportBlock(pos)) {
            return true;
        }
        return level.setBlock(pos, state, flags);
    }

    private boolean dmzrevamp$isProtectedSupportBlock(BlockPos pos) {
        AbstractKiProjectile projectile = (AbstractKiProjectile) (Object) this;
        return projectile.getOwner() instanceof ServerPlayer player && KiBlockProtection.isPlayerSupportArea(player, pos);
    }
}
