package com.dmzrevamp.network;

import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// Empty client-to-server packet used when the client asks to drop power release back to zero.
public record SetReleaseZeroC2SPacket() {
    // No payload is needed because the server can identify the sending player from the packet context.
    public static void encode(SetReleaseZeroC2SPacket packet, FriendlyByteBuf buffer) {
    }

    // Recreates the marker packet from the empty network buffer.
    public static SetReleaseZeroC2SPacket decode(FriendlyByteBuf buffer) {
        return new SetReleaseZeroC2SPacket();
    }

    // Drops power release only when the player is not transformed, then syncs the updated stats.
    public static void handle(SetReleaseZeroC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
                if (data.getCharacter().hasActiveForm()
                        || data.getCharacter().hasActiveStackForm()
                        || player.hasEffect(MainEffects.TRANSFORMED.get())
                        || player.hasEffect(MainEffects.STACK_TRANSFORMED.get())) {
                    return;
                }

                if (data.getResources().getPowerRelease() <= 0) {
                    return;
                }

                data.getResources().setPowerRelease(0);
                NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
            });
        });
        context.setPacketHandled(true);
    }
}
