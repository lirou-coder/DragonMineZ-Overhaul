package com.dmzrevamp.network;

import com.dmzrevamp.revamp.speed.SpeedLimitData;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SetSpeedLimitC2SPacket(int limit) {
    public static void encode(SetSpeedLimitC2SPacket packet, FriendlyByteBuf buffer) { buffer.writeVarInt(packet.limit); }
    public static SetSpeedLimitC2SPacket decode(FriendlyByteBuf buffer) { return new SetSpeedLimitC2SPacket(buffer.readVarInt()); }
    public static void handle(SetSpeedLimitC2SPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null) {
                StatsProvider.get(StatsCapability.INSTANCE, context.getSender()).ifPresent(data ->
                        ((SpeedLimitData) data).dmzrevamp$setSpeedLimit(packet.limit));
            }
        });
        context.setPacketHandled(true);
    }
}
