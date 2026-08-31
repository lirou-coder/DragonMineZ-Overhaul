package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.ki.KiClashTeams;
import com.dmzrevamp.revamp.strike.StrikeClashManager;
import com.dragonminez.common.combat.clash.BeamClash;
import com.dragonminez.common.combat.clash.BeamClashManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.dmzrevamp.revamp.ki.KiClashAttackResolver;
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
            KiClashTeams.syncHelpers();
        }
    }

    @Inject(method = "handlePlayerPress", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dmzrevamp$helperPress(ServerPlayer player, CallbackInfo ci) {
        if (StrikeClashManager.handlePlayerPress(player)) {
            ci.cancel();
            return;
        }
        if (KiClashTeams.handleHelperPress(player)) ci.cancel();
    }

    @ModifyConstant(method = "sendState", constant = @Constant(floatValue = 0.78F), remap = false)
    private static float dmzrevamp$syncGoodLow(float original) { return com.dmzrevamp.config.KiClashConfigured.get().goodAreaLow; }

    @ModifyConstant(method = "sendState", constant = @Constant(floatValue = 0.96F), remap = false)
    private static float dmzrevamp$syncGoodHigh(float original) { return com.dmzrevamp.config.KiClashConfigured.get().goodAreaHigh; }

    @Redirect(method = "onLevelTick", at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/init/entities/ki/AbstractKiProjectile;getClashRole()Lcom/dragonminez/common/init/entities/ki/AbstractKiProjectile$ClashRole;"), remap = false)
    private static AbstractKiProjectile.ClashRole dmzrevamp$configuredRole(AbstractKiProjectile projectile) {
        return KiClashAttackResolver.isAllowed(projectile) && KiClashAttackResolver.isLaunched(projectile)
                ? AbstractKiProjectile.ClashRole.MAJOR : AbstractKiProjectile.ClashRole.NONE;
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
