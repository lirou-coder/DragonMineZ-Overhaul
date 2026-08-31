package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.ki.KiClashTeams;
import com.dragonminez.common.network.C2S.KiBlastC2S;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(KiBlastC2S.class)
public abstract class PostClashKiBlastPacketMixin {
    @Inject(method = "handle", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dmzrevamp$blockWhilePunished(KiBlastC2S packet,
                                                      Supplier<NetworkEvent.Context> contextSupplier,
                                                      CallbackInfo ci) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null && KiClashTeams.isAbilityRestricted(player)) {
            context.setPacketHandled(true);
            ci.cancel();
        }
    }
}
