package com.dmzrevamp.network;

import com.dmzrevamp.revamp.classes.DmzClassConfigManager;
import com.dragonminez.common.config.ConfigManager;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.lang.reflect.Method;
import java.util.function.Supplier;

public record SaveSduClassC2SPacket(String classId, String json) {
    public static void encode(SaveSduClassC2SPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.classId, 128);
        buffer.writeUtf(packet.json, 32767);
    }

    public static SaveSduClassC2SPacket decode(FriendlyByteBuf buffer) {
        return new SaveSduClassC2SPacket(buffer.readUtf(128), buffer.readUtf(32767));
    }

    public static void handle(SaveSduClassC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !player.hasPermissions(2)) {
                return;
            }

            try {
                JsonObject json = JsonParser.parseString(packet.json).getAsJsonObject();
                if (!DmzClassConfigManager.saveEditableClassJson(packet.classId, json)) {
                    return;
                }
                ConfigManager.reload();
                resyncSdu(player);
            } catch (RuntimeException ignored) {
            }
        });
        context.setPacketHandled(true);
    }

    private static void resyncSdu(ServerPlayer player) {
        try {
            Class<?> compat = Class.forName("net.shurui.dev.sdu.compat.DmzCompat");
            Method method = compat.getMethod("resyncConfigsToAll", net.minecraft.server.MinecraftServer.class);
            method.invoke(null, player.getServer());
        } catch (ReflectiveOperationException | LinkageError ignored) {
        }
    }
}
