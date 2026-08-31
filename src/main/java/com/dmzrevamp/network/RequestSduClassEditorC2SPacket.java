package com.dmzrevamp.network;

import com.dmzrevamp.revamp.classes.DmzClassConfigManager;
import com.google.gson.Gson;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public record RequestSduClassEditorC2SPacket(String classId, boolean newClass) {
    private static final Gson GSON = new Gson();

    public static void encode(RequestSduClassEditorC2SPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.classId == null ? "" : packet.classId, 128);
        buffer.writeBoolean(packet.newClass);
    }

    public static RequestSduClassEditorC2SPacket decode(FriendlyByteBuf buffer) {
        return new RequestSduClassEditorC2SPacket(buffer.readUtf(128), buffer.readBoolean());
    }

    public static void handle(RequestSduClassEditorC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !player.hasPermissions(2)) {
                return;
            }

            String classId = packet.newClass ? "new_class" : packet.classId;
            String json = GSON.toJson(DmzClassConfigManager.getEditableClassJson(classId));
            DmzRevampNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new OpenSduClassEditorS2CPacket(classId, json, packet.newClass));
        });
        context.setPacketHandled(true);
    }
}
