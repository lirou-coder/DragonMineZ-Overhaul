package com.dmzrevamp.network;

import com.dmzrevamp.revamp.combat.CombatFlightDashRushManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Sends the client's current DMZ lock-on target to the server for combat-flight dash validation. */
public record SyncLockOnTargetC2SPacket(int targetId) {
    public static void encode(SyncLockOnTargetC2SPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.targetId);
    }

    public static SyncLockOnTargetC2SPacket decode(FriendlyByteBuf buffer) {
        return new SyncLockOnTargetC2SPacket(buffer.readVarInt());
    }

    public static void handle(SyncLockOnTargetC2SPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) CombatFlightDashRushManager.updateLockTarget(player, packet.targetId);
        });
        context.setPacketHandled(true);
    }
}
