package com.dmzrevamp.network;

import com.dmzrevamp.client.PrestigeFusionFlashClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record PrestigeFusionFlashS2CPacket(int entityId) {
    public static void encode(PrestigeFusionFlashS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
    }

    public static PrestigeFusionFlashS2CPacket decode(FriendlyByteBuf buffer) {
        return new PrestigeFusionFlashS2CPacket(buffer.readVarInt());
    }

    public static void handle(PrestigeFusionFlashS2CPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> PrestigeFusionFlashClient.trigger(packet.entityId)));
        context.setPacketHandled(true);
    }
}
