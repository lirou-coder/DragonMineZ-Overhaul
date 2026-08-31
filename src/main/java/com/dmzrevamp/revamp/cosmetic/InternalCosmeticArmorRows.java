package com.dmzrevamp.revamp.cosmetic;

import com.dmzrevamp.network.DmzRevampNetwork;
import com.dmzrevamp.network.InternalCosmeticArmorSyncS2CPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InternalCosmeticArmorRows {
    public static final int ROW_FUSION = 0;
    public static final int ROW_EXTERNAL = 1;
    public static final int ROW_COUNT = 2;

    private static final Map<UUID, RowSet> SERVER_ROWS = new ConcurrentHashMap<>();
    private static final Map<UUID, RowSet> CLIENT_ROWS = new ConcurrentHashMap<>();

    private InternalCosmeticArmorRows() {
    }

    public static ItemStack getClientStack(Player player, int row, EquipmentSlot slot) {
        RowSet rows = CLIENT_ROWS.get(player.getUUID());
        return rows != null ? rows.get(row, slot) : ItemStack.EMPTY;
    }

    public static boolean isClientEnabled(Player player, int row, EquipmentSlot slot) {
        RowSet rows = CLIENT_ROWS.get(player.getUUID());
        return rows != null && rows.isEnabled(row, slot);
    }

    public static boolean hasClientRow(Player player, int row) {
        RowSet rows = CLIENT_ROWS.get(player.getUUID());
        return rows != null && rows.hasEnabledSlot(row);
    }

    public static ItemStack getClientOverride(Player player, EquipmentSlot slot) {
        if (isClientEnabled(player, ROW_EXTERNAL, slot)) {
            return getClientStack(player, ROW_EXTERNAL, slot).copy();
        }
        if (isClientEnabled(player, ROW_FUSION, slot)) {
            return getClientStack(player, ROW_FUSION, slot).copy();
        }
        return null;
    }

    public static void set(ServerPlayer player, int row, EquipmentSlot slot, ItemStack stack) {
        set(player, row, slot, stack, !stack.isEmpty());
    }

    public static void set(ServerPlayer player, int row, EquipmentSlot slot, ItemStack stack, boolean enabled) {
        if (!isArmorSlot(slot) || row < 0 || row >= ROW_COUNT) {
            return;
        }
        RowSet rows = SERVER_ROWS.computeIfAbsent(player.getUUID(), ignored -> new RowSet());
        rows.set(row, slot, stack, enabled);
        sync(player);
    }

    public static void setRow(ServerPlayer player, int row, ItemStack boots, ItemStack leggings, ItemStack chestplate, ItemStack helmet, boolean hideHelmet) {
        if (row < 0 || row >= ROW_COUNT) {
            return;
        }
        RowSet rows = SERVER_ROWS.computeIfAbsent(player.getUUID(), ignored -> new RowSet());
        rows.set(row, EquipmentSlot.FEET, boots, !boots.isEmpty());
        rows.set(row, EquipmentSlot.LEGS, leggings, !leggings.isEmpty());
        rows.set(row, EquipmentSlot.CHEST, chestplate, !chestplate.isEmpty());
        rows.set(row, EquipmentSlot.HEAD, helmet, hideHelmet || !helmet.isEmpty());
        sync(player);
    }

    public static void clearRow(ServerPlayer player, int row) {
        if (row < 0 || row >= ROW_COUNT) {
            return;
        }
        RowSet rows = SERVER_ROWS.computeIfAbsent(player.getUUID(), ignored -> new RowSet());
        rows.clearRow(row);
        sync(player);
    }

    public static void clearAll(ServerPlayer player) {
        SERVER_ROWS.remove(player.getUUID());
        sync(player, new RowSet());
    }

    public static RowSet copyServerRows(ServerPlayer player) {
        RowSet rows = SERVER_ROWS.get(player.getUUID());
        return rows != null ? rows.copy() : new RowSet();
    }

    public static void applyClientRows(UUID playerId, RowSet rows) {
        if (rows == null || rows.isEmpty()) {
            CLIENT_ROWS.remove(playerId);
        } else {
            CLIENT_ROWS.put(playerId, rows.copy());
        }
    }

    public static void sync(ServerPlayer player) {
        sync(player, copyServerRows(player));
    }

    private static void sync(ServerPlayer player, RowSet rows) {
        DmzRevampNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), new InternalCosmeticArmorSyncS2CPacket(player.getUUID(), rows));
    }

    private static boolean isArmorSlot(EquipmentSlot slot) {
        return slot == EquipmentSlot.FEET || slot == EquipmentSlot.LEGS || slot == EquipmentSlot.CHEST || slot == EquipmentSlot.HEAD;
    }

    public static int slotIndex(EquipmentSlot slot) {
        return switch (slot) {
            case FEET -> 0;
            case LEGS -> 1;
            case CHEST -> 2;
            case HEAD -> 3;
            default -> -1;
        };
    }

    public static EquipmentSlot slotByIndex(int index) {
        return switch (index) {
            case 0 -> EquipmentSlot.FEET;
            case 1 -> EquipmentSlot.LEGS;
            case 2 -> EquipmentSlot.CHEST;
            case 3 -> EquipmentSlot.HEAD;
            default -> EquipmentSlot.MAINHAND;
        };
    }

    public static final class RowSet {
        private final SlotState[][] slots = new SlotState[ROW_COUNT][4];

        public RowSet() {
            for (int row = 0; row < ROW_COUNT; row++) {
                for (int slot = 0; slot < 4; slot++) {
                    slots[row][slot] = new SlotState(false, ItemStack.EMPTY);
                }
            }
        }

        public boolean isEnabled(int row, EquipmentSlot slot) {
            int index = slotIndex(slot);
            return row >= 0 && row < ROW_COUNT && index >= 0 && slots[row][index].enabled;
        }

        public ItemStack get(int row, EquipmentSlot slot) {
            int index = slotIndex(slot);
            return row >= 0 && row < ROW_COUNT && index >= 0 ? slots[row][index].stack : ItemStack.EMPTY;
        }

        public void set(int row, EquipmentSlot slot, ItemStack stack, boolean enabled) {
            int index = slotIndex(slot);
            if (row < 0 || row >= ROW_COUNT || index < 0) {
                return;
            }
            slots[row][index] = new SlotState(enabled, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
        }

        public void clearRow(int row) {
            if (row < 0 || row >= ROW_COUNT) {
                return;
            }
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD}) {
                set(row, slot, ItemStack.EMPTY, false);
            }
        }

        public boolean hasEnabledSlot(int row) {
            if (row < 0 || row >= ROW_COUNT) {
                return false;
            }
            for (int slot = 0; slot < 4; slot++) {
                if (slots[row][slot].enabled) {
                    return true;
                }
            }
            return false;
        }

        public boolean isEmpty() {
            for (int row = 0; row < ROW_COUNT; row++) {
                if (hasEnabledSlot(row)) {
                    return false;
                }
            }
            return true;
        }

        public RowSet copy() {
            RowSet copy = new RowSet();
            for (int row = 0; row < ROW_COUNT; row++) {
                for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD}) {
                    int index = slotIndex(slot);
                    SlotState state = slots[row][index];
                    copy.slots[row][index] = new SlotState(state.enabled, state.stack.isEmpty() ? ItemStack.EMPTY : state.stack.copy());
                }
            }
            return copy;
        }

        public Map<EquipmentSlot, ItemStack> enabledStacks(int row) {
            Map<EquipmentSlot, ItemStack> stacks = new EnumMap<>(EquipmentSlot.class);
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD}) {
                if (isEnabled(row, slot)) {
                    stacks.put(slot, get(row, slot));
                }
            }
            return stacks;
        }
    }

    private record SlotState(boolean enabled, ItemStack stack) {
    }
}
