package com.dmzrevamp.network;

import com.dmzrevamp.client.ClientKiClashMeterState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record KiClashMeterConfigS2CPacket(float speedMultiplier, float areaSizeMultiplier,
                                           float perfectFraction, float minimumGoodEfficiency) {
    public static void encode(KiClashMeterConfigS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.speedMultiplier);
        buffer.writeFloat(packet.areaSizeMultiplier);
        buffer.writeFloat(packet.perfectFraction);
        buffer.writeFloat(packet.minimumGoodEfficiency);
    }

    public static KiClashMeterConfigS2CPacket decode(FriendlyByteBuf buffer) {
        return new KiClashMeterConfigS2CPacket(
                buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
    }

    public static void handle(KiClashMeterConfigS2CPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientKiClashMeterState.update(packet.speedMultiplier,
                        packet.areaSizeMultiplier, packet.perfectFraction,
                        packet.minimumGoodEfficiency)));
        context.setPacketHandled(true);
    }
}
