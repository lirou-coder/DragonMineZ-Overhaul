package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.ki.KiClashTeams;
import com.dragonminez.common.network.C2S.DashC2S;
import com.dragonminez.common.network.C2S.ExecuteActionC2S;
import com.dragonminez.common.network.C2S.InstantTransmissionTapC2S;
import com.dragonminez.common.network.C2S.InstantTransmissionTravelC2S;
import com.dragonminez.common.network.C2S.InstantTransmissionTravelToPlayerC2S;
import com.dragonminez.common.network.C2S.SokidanControlC2S;
import com.dragonminez.common.network.C2S.StrikeAttackC2S;
import com.dragonminez.common.network.C2S.TaiyokenCastC2S;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin({StrikeAttackC2S.class, DashC2S.class, ExecuteActionC2S.class,
        InstantTransmissionTapC2S.class, InstantTransmissionTravelC2S.class,
        InstantTransmissionTravelToPlayerC2S.class, SokidanControlC2S.class,
        TaiyokenCastC2S.class})
public abstract class PostClashStrikeAttackPacketMixin {
    @Inject(method = "handle", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzrevamp$blockWhilePunished(Supplier<NetworkEvent.Context> contextSupplier, CallbackInfo ci) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null && KiClashTeams.isAbilityRestricted(player)) {
            context.setPacketHandled(true);
            ci.cancel();
        }
    }
}
