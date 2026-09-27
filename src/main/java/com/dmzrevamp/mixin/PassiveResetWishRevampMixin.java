package com.dmzrevamp.mixin;

import com.dmzrevamp.racial.RevampRacialResetHelper;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.wish.wishes.PassiveResetWish;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PassiveResetWish.class)
public abstract class PassiveResetWishRevampMixin {
    @Inject(method = "grant", at = @At("TAIL"), remap = false)
    private void dmzrevamp$resetRevampRacialPassives(ServerPlayer player, CallbackInfo ci) {
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            RevampRacialResetHelper.resetAll(player, data);
        });
    }
}
