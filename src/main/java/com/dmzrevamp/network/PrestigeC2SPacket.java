package com.dmzrevamp.network;

import com.dmzrevamp.revamp.prestige.PrestigeService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record PrestigeC2SPacket() {
    public static void encode(PrestigeC2SPacket packet, FriendlyByteBuf buffer) {
    }

    public static PrestigeC2SPacket decode(FriendlyByteBuf buffer) {
        return new PrestigeC2SPacket();
    }

    public static void handle(PrestigeC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) PrestigeService.tryPrestige(player);
        });
        context.setPacketHandled(true);
    }
}
