package com.dmzrevamp.network;

import com.dmzrevamp.revamp.combat.LegacyComboManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Arms the next ordinary melee hit for the restored DMZ 2.0.4 combo. */
public record LegacyComboAttackC2SPacket() {
    public static void encode(LegacyComboAttackC2SPacket packet, FriendlyByteBuf buffer) {
    }

    public static LegacyComboAttackC2SPacket decode(FriendlyByteBuf buffer) {
        return new LegacyComboAttackC2SPacket();
    }

    public static void handle(LegacyComboAttackC2SPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) LegacyComboManager.arm(player);
        });
        context.setPacketHandled(true);
    }
}
