package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.ki.KiClashTeams;
import com.dragonminez.common.network.C2S.TechniqueChargeC2S;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(TechniqueChargeC2S.class)
public abstract class PostClashTechniqueChargePacketMixin {
    @Inject(method = "handle", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dmzrevamp$blockWhilePunished(TechniqueChargeC2S packet,
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
