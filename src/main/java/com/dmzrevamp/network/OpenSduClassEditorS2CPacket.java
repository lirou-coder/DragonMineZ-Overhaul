package com.dmzrevamp.network;

import com.dmzrevamp.compat.sdu.client.SduClassEditorClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record OpenSduClassEditorS2CPacket(String classId, String json, boolean newClass) {
    public static void encode(OpenSduClassEditorS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.classId, 128);
        buffer.writeUtf(packet.json, 32767);
        buffer.writeBoolean(packet.newClass);
    }

    public static OpenSduClassEditorS2CPacket decode(FriendlyByteBuf buffer) {
        return new OpenSduClassEditorS2CPacket(buffer.readUtf(128), buffer.readUtf(32767), buffer.readBoolean());
    }

    public static void handle(OpenSduClassEditorS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> SduClassEditorClient.open(packet.classId, packet.json, packet.newClass)));
        context.setPacketHandled(true);
    }
}
