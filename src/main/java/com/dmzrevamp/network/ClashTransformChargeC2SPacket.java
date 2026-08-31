package com.dmzrevamp.network;

import com.dmzrevamp.revamp.ki.KiClashTransformationBridge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Carries the physical ACTION state independently of DMZ's clash input lock. */
public record ClashTransformChargeC2SPacket(boolean held) {
    public static void encode(ClashTransformChargeC2SPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.held);
    }

    public static ClashTransformChargeC2SPacket decode(FriendlyByteBuf buffer) {
        return new ClashTransformChargeC2SPacket(buffer.readBoolean());
    }

    public static void handle(ClashTransformChargeC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                KiClashTransformationBridge.updateInput(player, packet.held);
            }
        });
        context.setPacketHandled(true);
    }
}
