package com.dmzrevamp.network;

import com.dmzrevamp.revamp.strike.StrikeClashManager;
import com.dragonminez.common.network.PacketRateLimiter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Independent right-click route for Strike Clash; shares DMZ's limiter with Beam Clash input. */
public final class StrikeClashInputC2SPacket {
    public static void encode(StrikeClashInputC2SPacket packet, FriendlyByteBuf buffer) {
    }

    public static StrikeClashInputC2SPacket decode(FriendlyByteBuf buffer) {
        return new StrikeClashInputC2SPacket();
    }

    public static void handle(StrikeClashInputC2SPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            if (!PacketRateLimiter.allow(player.getUUID(), "beam_clash_press",
                    player.level().getGameTime(), 1L)) return;
            StrikeClashManager.handlePlayerPress(player);
        });
        context.setPacketHandled(true);
    }
}
