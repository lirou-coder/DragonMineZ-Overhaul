package com.dmzrevamp.network;

import com.dmzrevamp.revamp.cosmetic.InternalCosmeticArmorRows;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record InternalCosmeticArmorSyncS2CPacket(UUID playerId, InternalCosmeticArmorRows.RowSet rows) {
    public static void encode(InternalCosmeticArmorSyncS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.playerId);
        for (int row = 0; row < InternalCosmeticArmorRows.ROW_COUNT; row++) {
            for (int slotIndex = 0; slotIndex < 4; slotIndex++) {
                EquipmentSlot slot = InternalCosmeticArmorRows.slotByIndex(slotIndex);
                boolean enabled = packet.rows.isEnabled(row, slot);
                buffer.writeBoolean(enabled);
                buffer.writeItem(enabled ? packet.rows.get(row, slot) : ItemStack.EMPTY);
            }
        }
    }

    public static InternalCosmeticArmorSyncS2CPacket decode(FriendlyByteBuf buffer) {
        UUID playerId = buffer.readUUID();
        InternalCosmeticArmorRows.RowSet rows = new InternalCosmeticArmorRows.RowSet();
        for (int row = 0; row < InternalCosmeticArmorRows.ROW_COUNT; row++) {
            for (int slotIndex = 0; slotIndex < 4; slotIndex++) {
                boolean enabled = buffer.readBoolean();
                ItemStack stack = buffer.readItem();
                rows.set(row, InternalCosmeticArmorRows.slotByIndex(slotIndex), stack, enabled);
            }
        }
        return new InternalCosmeticArmorSyncS2CPacket(playerId, rows);
    }

    public static void handle(InternalCosmeticArmorSyncS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> InternalCosmeticArmorRows.applyClientRows(packet.playerId, packet.rows)));
        context.setPacketHandled(true);
    }
}
