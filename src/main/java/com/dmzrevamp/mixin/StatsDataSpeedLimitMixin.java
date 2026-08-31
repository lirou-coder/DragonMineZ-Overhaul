package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.speed.SpeedLimitData;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StatsData.class)
public abstract class StatsDataSpeedLimitMixin implements SpeedLimitData {
    @Unique private static final String DMZREVAMP_SPEED_LIMIT = "dmzrevampSpeedLimit";
    @Unique private int dmzrevamp$speedLimit;

    @Override public int dmzrevamp$getSpeedLimit() { return dmzrevamp$speedLimit; }
    @Override public void dmzrevamp$setSpeedLimit(int limit) {
        dmzrevamp$speedLimit = limit <= 0 ? 0 : Math.max(100, Math.min(3000, limit));
    }

    @Inject(method = "save", at = @At("RETURN"), remap = false)
    private void dmzrevamp$saveSpeedLimit(CallbackInfoReturnable<CompoundTag> cir) {
        cir.getReturnValue().putInt(DMZREVAMP_SPEED_LIMIT, dmzrevamp$speedLimit);
    }

    @Inject(method = "load", at = @At("TAIL"), remap = false)
    private void dmzrevamp$loadSpeedLimit(CompoundTag tag, CallbackInfo ci) {
        dmzrevamp$setSpeedLimit(tag.getInt(DMZREVAMP_SPEED_LIMIT));
    }

    @Inject(method = "copyFrom", at = @At("TAIL"), remap = false)
    private void dmzrevamp$copySpeedLimit(StatsData source, CallbackInfo ci) {
        dmzrevamp$speedLimit = ((SpeedLimitData) source).dmzrevamp$getSpeedLimit();
    }
}
