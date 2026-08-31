package com.dmzrevamp.network;

import com.dmzrevamp.client.ClientStrikeClashState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record StrikeClashModeS2CPacket(int entityId, boolean active) {
    public static void encode(StrikeClashModeS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeBoolean(packet.active);
    }

    public static StrikeClashModeS2CPacket decode(FriendlyByteBuf buffer) {
        return new StrikeClashModeS2CPacket(buffer.readVarInt(), buffer.readBoolean());
    }

    public static void handle(StrikeClashModeS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientStrikeClashState.setEntityActive(packet.entityId, packet.active)));
        context.setPacketHandled(true);
    }
}
