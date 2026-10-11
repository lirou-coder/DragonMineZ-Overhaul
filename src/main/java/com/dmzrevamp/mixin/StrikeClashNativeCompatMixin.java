package com.dmzrevamp.mixin;

import com.dmzrevamp.config.StrikeClashConfigured;
import com.dmzrevamp.revamp.strike.FlyingStrikeYLock;
import com.dmzrevamp.revamp.strike.StrikeClashManager;
import com.dmzrevamp.mixin.DBSagasEntityComboTargetAccessor;
import com.dragonminez.common.combat.clash.StrikeClash;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.dragonminez.server.events.players.combat.StrikeAttackHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Restricts and tunes DMZ 2.2's own Strike Clash without replacing its lifecycle. */
@Mixin(value = StrikeClash.class, remap = false)
public abstract class StrikeClashNativeCompatMixin {
    @Inject(method = "tryStart", at = @At("HEAD"), cancellable = true)
    private static void dmzrevamp$applyClashEligibility(LivingEntity self, CallbackInfoReturnable<Boolean> cir) {
        if (!StrikeClashConfigured.get().enabled
                || (self instanceof ServerPlayer player && !StrikeClashManager.canPlayerStartNativeClash(player))) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isRushing", at = @At("RETURN"), cancellable = true)
    private static void dmzrevamp$filterRushCandidate(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof ServerPlayer player) {
            cir.setReturnValue(StrikeClashManager.canPlayerStartNativeClash(player));
        } else if (entity instanceof DBSagasEntity saga) {
            cir.setReturnValue(StrikeClashManager.canNpcStartNativeClash(saga));
        }
    }

    @org.spongepowered.asm.mixin.injection.Redirect(method = "start", at = @At(value = "INVOKE",
            target = "Lcom/dragonminez/server/events/players/combat/StrikeAttackHandler;takeRushForClash(Lnet/minecraft/server/level/ServerPlayer;)Lcom/dragonminez/server/events/players/combat/StrikeAttackHandler$ClashRush;"), require = 1)
    private static StrikeAttackHandler.ClashRush dmzrevamp$includeRecentlyStartedStrike(ServerPlayer player) {
        return StrikeClashManager.takePlayerRushForNativeClash(player);
    }

    @org.spongepowered.asm.mixin.injection.Redirect(method = "headingTowards", at = @At(value = "INVOKE",
            target = "Lcom/dragonminez/server/events/players/combat/StrikeAttackHandler;rushTargetId(Lnet/minecraft/server/level/ServerPlayer;)Ljava/util/UUID;"), require = 1)
    private static java.util.UUID dmzrevamp$useActiveStrikeTarget(ServerPlayer player) {
        return StrikeClashManager.nativePlayerStrikeTarget(player);
    }

    @org.spongepowered.asm.mixin.injection.Redirect(method = "headingTowards", at = @At(value = "INVOKE",
            target = "Lcom/dragonminez/common/init/entities/sagas/DBSagasEntity;getStrikeRushTarget()Lnet/minecraft/world/entity/LivingEntity;"), require = 1)
    private static LivingEntity dmzrevamp$useTargetedNpcComboTarget(DBSagasEntity saga) {
        LivingEntity rushTarget = saga.getStrikeRushTarget();
        if (rushTarget != null || !StrikeClashManager.canNpcStartNativeClash(saga)) return rushTarget;
        return ((DBSagasEntityComboTargetAccessor) saga).dmzrevamp$getComboTarget();
    }

    @Inject(method = "start", at = @At("RETURN"))
    private static void dmzrevamp$syncNativeClashMode(ServerLevel level, LivingEntity first,
                                                       LivingEntity second, CallbackInfo ci) {
        StrikeClashManager.beginNativeClash(first, second);
    }

    @Inject(method = "start", at = @At("HEAD"))
    private static void dmzrevamp$suspendStrikeYLock(ServerLevel level, LivingEntity first,
                                                      LivingEntity second, CallbackInfo ci) {
        if (first instanceof ServerPlayer player) FlyingStrikeYLock.suspendForClash(player);
        if (second instanceof ServerPlayer player) FlyingStrikeYLock.suspendForClash(player);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void dmzrevamp$refreshNativeClashStats(CallbackInfoReturnable<?> cir) {
        StrikeClashManager.refreshNativeClashState(this);
    }

    @Inject(method = "release", at = @At("HEAD"))
    private void dmzrevamp$clearNativeParticipant(@Coerce Object side, Boolean won, CallbackInfo ci) {
        Object entity = readSideEntity(side);
        if (entity instanceof ServerPlayer player) FlyingStrikeYLock.endClash(player);
        StrikeClashManager.endNativeClashSide(side);
    }

    @Unique
    private static Object readSideEntity(Object side) {
        try {
            java.lang.reflect.Field field = side.getClass().getDeclaredField("entity");
            field.setAccessible(true);
            return field.get(side);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    @Inject(method = "power", at = @At("HEAD"), cancellable = true)
    private static void dmzrevamp$useMeleeOnly(LivingEntity entity, CallbackInfoReturnable<Double> cir) {
        if (!(entity instanceof ServerPlayer player)) return;
        double melee = com.dragonminez.common.stats.StatsProvider
                .get(com.dragonminez.common.stats.StatsCapability.INSTANCE, player)
                .map(data -> data.getMeleeDamage()).orElse(1D);
        double power = StrikeClashManager.configuredMeleePower(melee);
        cir.setReturnValue(Double.isFinite(power) ? Math.max(1D, power) : 1D);
    }

    @ModifyConstant(method = "tick", constant = @Constant(intValue = 240))
    private int dmzrevamp$configuredDuration(int original) {
        return StrikeClashConfigured.get().maxClashDurationTicks;
    }
}
