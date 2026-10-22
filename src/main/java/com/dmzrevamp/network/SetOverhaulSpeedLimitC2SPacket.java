package com.dmzrevamp.network;

import com.dmzrevamp.revamp.OverhaulSpeedLimitState;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public record SetOverhaulSpeedLimitC2SPacket(int value) {
    public static void encode(SetOverhaulSpeedLimitC2SPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.value);
    }

    public static SetOverhaulSpeedLimitC2SPacket decode(FriendlyByteBuf buffer) {
        return new SetOverhaulSpeedLimitC2SPacket(buffer.readVarInt());
    }

    public static void handle(SetOverhaulSpeedLimitC2SPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
                if (data.getSkills().getSkillLevel("sprint") <= 0) return;
                int value = OverhaulSpeedLimitState.set(player, packet.value);
                DmzRevampNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        new SyncOverhaulSpeedLimitS2CPacket(value));
            });
        });
        context.setPacketHandled(true);
    }
}
