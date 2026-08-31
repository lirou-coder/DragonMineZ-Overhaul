package com.dmzrevamp.network;

import com.dmzrevamp.client.CombatFlightDoubleDashImpulseClientEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Explicit signal for a real DMZ dash; cooldown effects are intentionally not used as a proxy. */
public record CombatFlightDashImpulseS2CPacket(int directionId) {
    public static void encode(CombatFlightDashImpulseS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeByte(packet.directionId);
    }

    public static CombatFlightDashImpulseS2CPacket decode(FriendlyByteBuf buffer) {
        return new CombatFlightDashImpulseS2CPacket(buffer.readByte());
    }

    public static void handle(CombatFlightDashImpulseS2CPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> CombatFlightDoubleDashImpulseClientEvents.applyServerDashImpulse(packet.directionId)
        ));
        context.setPacketHandled(true);
    }
}
