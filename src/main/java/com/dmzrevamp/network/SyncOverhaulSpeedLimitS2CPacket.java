package com.dmzrevamp.network;

import com.dmzrevamp.revamp.OverhaulSpeedLimitState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncOverhaulSpeedLimitS2CPacket(int value) {
    public static void encode(SyncOverhaulSpeedLimitS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.value);
    }

    public static SyncOverhaulSpeedLimitS2CPacket decode(FriendlyByteBuf buffer) {
        return new SyncOverhaulSpeedLimitS2CPacket(buffer.readVarInt());
    }

    public static void handle(SyncOverhaulSpeedLimitS2CPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> OverhaulSpeedLimitState.setClient(packet.value));
        context.setPacketHandled(true);
    }
}
