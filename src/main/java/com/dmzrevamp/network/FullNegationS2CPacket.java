package com.dmzrevamp.network;

import com.dmzrevamp.client.FullNegationClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record FullNegationS2CPacket() {
    public static void encode(FullNegationS2CPacket packet, FriendlyByteBuf buffer) {
    }

    public static FullNegationS2CPacket decode(FriendlyByteBuf buffer) {
        return new FullNegationS2CPacket();
    }

    public static void handle(FullNegationS2CPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(FullNegationClientState::suppressCurrentHit);
        context.setPacketHandled(true);
    }
}
