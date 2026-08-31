package com.dmzrevamp.network;

import com.dmzrevamp.client.ScouterClientState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncMasterStructureS2CPacket(BlockPos target) {
    public static void encode(SyncMasterStructureS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.target != null);
        if (packet.target != null) {
            buffer.writeBlockPos(packet.target);
        }
    }

    public static SyncMasterStructureS2CPacket decode(FriendlyByteBuf buffer) {
        return new SyncMasterStructureS2CPacket(buffer.readBoolean() ? buffer.readBlockPos() : null);
    }

    public static void handle(SyncMasterStructureS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ScouterClientState.setMasterTarget(packet.target)));
        context.setPacketHandled(true);
    }
}
