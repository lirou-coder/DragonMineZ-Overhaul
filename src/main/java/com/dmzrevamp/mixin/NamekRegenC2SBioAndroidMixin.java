package com.dmzrevamp.mixin;

import com.dmzrevamp.racial.CustomRacialActionHelper;
import com.dragonminez.common.network.C2S.NamekRegenC2S;
import com.dragonminez.common.racial.impl.NamekAssimilation;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(value = NamekRegenC2S.class, remap = false)
public abstract class NamekRegenC2SBioAndroidMixin {
    @Inject(method = "handle", at = @At("HEAD"))
    private void dmzrevamp$handleBioAndroidRegen(Supplier<NetworkEvent.Context> supplier, CallbackInfo ci) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
                if (data.getStatus().isStunned()) return;
                if (!"bioandroidrevamp".equalsIgnoreCase(CustomRacialActionHelper.getConfiguredRacialSkillId(data))) return;
                NamekAssimilation.startRegen(player, data);
            });
        });
    }
}
