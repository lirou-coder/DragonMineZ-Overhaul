package com.dmzrevamp.network;

import com.dmzrevamp.client.ClientStrikeClashState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record StrikeClashModeS2CPacket(int entityId, boolean active, float goodAreaMultiplier,
                                        float meterSpeedMultiplier, float areaSizeMultiplier,
                                        float perfectFraction, float minimumGoodEfficiency) {
    public static void encode(StrikeClashModeS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeBoolean(packet.active);
        buffer.writeFloat(packet.goodAreaMultiplier);
        buffer.writeFloat(packet.meterSpeedMultiplier);
        buffer.writeFloat(packet.areaSizeMultiplier);
        buffer.writeFloat(packet.perfectFraction);
        buffer.writeFloat(packet.minimumGoodEfficiency);
    }

    public static StrikeClashModeS2CPacket decode(FriendlyByteBuf buffer) {
        return new StrikeClashModeS2CPacket(buffer.readVarInt(), buffer.readBoolean(),
                buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
    }

    public static void handle(StrikeClashModeS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientStrikeClashState.setEntityActive(
                        packet.entityId, packet.active, packet.goodAreaMultiplier,
                        packet.meterSpeedMultiplier, packet.areaSizeMultiplier,
                        packet.perfectFraction, packet.minimumGoodEfficiency)));
        context.setPacketHandled(true);
    }
}
