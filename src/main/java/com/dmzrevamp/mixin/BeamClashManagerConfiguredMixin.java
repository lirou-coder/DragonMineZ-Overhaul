package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.ki.KiClashAttackResolver;
import com.dmzrevamp.revamp.ki.KiClashTeams;
import com.dragonminez.common.combat.clash.BeamClash;
import com.dragonminez.common.combat.clash.BeamClashManager;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(BeamClashManager.class)
public abstract class BeamClashManagerConfiguredMixin {
    @Shadow @Final private static List<BeamClash> ACTIVE_CLASHES;

    @Inject(method = "onLevelTick", at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/combat/clash/BeamClashManager;rebuildClashingOwners()V", shift = At.Shift.BEFORE), remap = false)
    private static void dmzrevamp$tickTeams(TickEvent.LevelTickEvent event, CallbackInfo ci) {
        if (event.level instanceof ServerLevel level) KiClashTeams.tick(level, ACTIVE_CLASHES);
    }

    @Inject(method = "onLevelTick", at = @At("TAIL"), remap = false)
    private static void dmzrevamp$syncHelpersLast(TickEvent.LevelTickEvent event, CallbackInfo ci) {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel level) {
            KiClashTeams.syncHelpers(level);
        }
    }

    /**
     * DMZ 2.2 sends the client-simulated press time and marker to the server. Helpers must be
     * validated through the same ClashParticipant path instead of the old parameterless press.
     * Strike Clash also reuses this native input route while its overlay is active.
     */
    @Inject(method = "handlePlayerPress", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dmzrevamp$customParticipantPress(ServerPlayer player, float pressTime, float marker, CallbackInfo ci) {
        if (com.dmzrevamp.revamp.strike.StrikeClashManager.handlePlayerPress(player, pressTime, marker)) {
            ci.cancel();
            return;
        }
        if (KiClashTeams.handleHelperPress(player, pressTime, marker)) ci.cancel();
    }

    /**
     * Upgrade Overhaul-enabled attack types to MAJOR without erasing DMZ 2.2's MINOR role.
     * Keeping MINOR matters because native major beams now shatter minor ki attacks on contact.
     */
    @Redirect(method = "onLevelTick", at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/init/entities/ki/AbstractKiProjectile;getClashRole()Lcom/dragonminez/common/init/entities/ki/AbstractKiProjectile$ClashRole;"), remap = false)
    private static AbstractKiProjectile.ClashRole dmzrevamp$configuredRole(AbstractKiProjectile projectile) {
        AbstractKiProjectile.ClashRole nativeRole = projectile.getClashRole();
        if (KiClashAttackResolver.isAllowed(projectile) && KiClashAttackResolver.isLaunched(projectile)) {
            return AbstractKiProjectile.ClashRole.MAJOR;
        }
        return nativeRole == AbstractKiProjectile.ClashRole.MAJOR
                ? AbstractKiProjectile.ClashRole.NONE
                : nativeRole;
    }

    @Inject(method = "beamsClash", at = @At("RETURN"), cancellable = true, remap = false)
    private static void dmzrevamp$cancelOverwhelmingClash(AbstractKiProjectile first, AbstractKiProjectile second,
                                                          CallbackInfoReturnable<Boolean> cir) {
        if (first.isRemoved() || second.isRemoved()) {
            cir.setReturnValue(false);
            return;
        }
        if (cir.getReturnValueZ() && KiClashTeams.cancelIfTooStrong(first, second)) cir.setReturnValue(false);
    }

    @Inject(method = "beamsClash", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dmzrevamp$visualSphereCollision(AbstractKiProjectile first, AbstractKiProjectile second,
                                                         CallbackInfoReturnable<Boolean> cir) {
        if (KiClashTeams.visualSphereClash(first, second)) cir.setReturnValue(true);
    }
}
