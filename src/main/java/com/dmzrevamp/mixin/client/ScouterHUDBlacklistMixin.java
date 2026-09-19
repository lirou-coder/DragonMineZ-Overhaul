package com.dmzrevamp.mixin.client;

import com.dmzrevamp.config.KiSenseBlacklistConfig;
import com.dragonminez.client.gui.hud.ScouterHUD;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = ScouterHUD.class, remap = false)
public abstract class ScouterHUDBlacklistMixin {
    @Shadow private static int strongestEntityID;
    @Shadow private static double cachedBP;

    @Inject(method = "performSmartScan", at = @At("HEAD"), require = 0)
    private static void dmzrevamp$dropCachedBlacklistedTarget(net.minecraft.world.entity.player.Player player,
                                                              org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        net.minecraft.world.entity.Entity current = player.level().getEntity(strongestEntityID);
        if (KiSenseBlacklistConfig.contains(current)) {
            strongestEntityID = -1;
            cachedBP = 0D;
        }
    }

    @Inject(method = "getEntityBP", at = @At("HEAD"), cancellable = true, require = 0)
    private static void dmzrevamp$blacklistedEntitiesHaveNoScouterBp(LivingEntity entity,
                                                                      CallbackInfoReturnable<Double> cir) {
        if (KiSenseBlacklistConfig.contains(entity)) cir.setReturnValue(-1D);
    }

    @ModifyVariable(method = "performSmartScan", at = @At("STORE"), ordinal = 0, require = 0)
    private static List<LivingEntity> dmzrevamp$removeBlacklistedScouterTargets(List<LivingEntity> original) {
        if (original == null || original.isEmpty()) return original;
        List<LivingEntity> filtered = new ArrayList<>(original);
        filtered.removeIf(KiSenseBlacklistConfig::contains);
        return filtered;
    }
}
