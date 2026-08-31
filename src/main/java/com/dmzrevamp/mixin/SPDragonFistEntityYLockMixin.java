package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.strike.FlyingStrikeYLock;
import com.dragonminez.common.init.entities.ki.SPDragonFistEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = SPDragonFistEntity.class, remap = false)
public abstract class SPDragonFistEntityYLockMixin {
    @Redirect(
            method = {"tick", "m_8119_"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;m_20256_(Lnet/minecraft/world/phys/Vec3;)V"
            ),
            remap = false,
            require = 1,
            expect = 1
    )
    private void dmzrevamp$preserveHorizontalDragonFistCharge(Entity owner, Vec3 movement) {
        if (!(owner instanceof Player player) || !FlyingStrikeYLock.isLocked(player)
                || movement.lengthSqr() < 1.0E-10D) {
            owner.setDeltaMovement(movement);
            return;
        }

        Vec3 horizontal = new Vec3(movement.x, 0D, movement.z);
        if (horizontal.horizontalDistanceSqr() < 1.0E-8D) {
            float yaw = player.getYHeadRot() * ((float) Math.PI / 180F);
            horizontal = new Vec3(-Math.sin(yaw), 0D, Math.cos(yaw));
        }
        FlyingStrikeYLock.applyScriptedHorizontalDisplacement(player, horizontal.normalize().scale(2.5D));
    }
}
