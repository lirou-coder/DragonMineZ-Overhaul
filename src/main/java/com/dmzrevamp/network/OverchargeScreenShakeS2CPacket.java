package com.dmzrevamp.network;

import com.dmzrevamp.client.OverchargeScreenShakeClientEffects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record OverchargeScreenShakeS2CPacket(float intensity, int ticks) {
    public static void encode(OverchargeScreenShakeS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.intensity);
        buffer.writeVarInt(packet.ticks);
    }

    public static OverchargeScreenShakeS2CPacket decode(FriendlyByteBuf buffer) {
        return new OverchargeScreenShakeS2CPacket(buffer.readFloat(), buffer.readVarInt());
    }

    public static void handle(OverchargeScreenShakeS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> OverchargeScreenShakeClientEffects.start(packet.intensity, packet.ticks)));
        context.setPacketHandled(true);
    }
}
