package com.dmzrevamp.network;

import com.dmzrevamp.client.StrikeYAnchorClientHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record StrikeYAnchorS2CPacket(double anchorY, int remainingTicks) {
    public static void encode(StrikeYAnchorS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.anchorY);
        buffer.writeVarInt(packet.remainingTicks);
    }

    public static StrikeYAnchorS2CPacket decode(FriendlyByteBuf buffer) {
        return new StrikeYAnchorS2CPacket(buffer.readDouble(), buffer.readVarInt());
    }

    public static void handle(StrikeYAnchorS2CPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> StrikeYAnchorClientHandler.apply(packet.anchorY, packet.remainingTicks)
        ));
        context.setPacketHandled(true);
    }
}
