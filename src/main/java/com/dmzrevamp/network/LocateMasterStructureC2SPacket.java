package com.dmzrevamp.network;

import com.dragonminez.server.world.structure.helper.StructureLocator;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.function.Supplier;
import org.slf4j.Logger;

public record LocateMasterStructureC2SPacket() {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final List<ResourceLocation> MASTER_STRUCTURES = List.of(
            ResourceLocation.fromNamespaceAndPath("dragonminez", "goku_house"),
            ResourceLocation.fromNamespaceAndPath("dragonminez", "roshi_house"),
            ResourceLocation.fromNamespaceAndPath("dragonminez", "kamilookout"),
            ResourceLocation.fromNamespaceAndPath("dragonminez", "elder_guru"),
            ResourceLocation.fromNamespaceAndPath("dragonminez", "piccolo_house"),
            ResourceLocation.fromNamespaceAndPath("dragonminez", "yamcha_house"),
            ResourceLocation.fromNamespaceAndPath("dragonminez", "oldkai_pillar"),
            ResourceLocation.fromNamespaceAndPath("dragonminez", "timechamber"),
            ResourceLocation.fromNamespaceAndPath("dragonminez", "babidi"),
            ResourceLocation.fromNamespaceAndPath("dragonminez", "cell_arena"),
            ResourceLocation.fromNamespaceAndPath("dragonminez", "frieza_ship"),
            ResourceLocation.fromNamespaceAndPath("dragonminez", "gero_lab"),
            ResourceLocation.fromNamespaceAndPath("dragonminez", "vegeta_pod")
    );

    public static void encode(LocateMasterStructureC2SPacket packet, FriendlyByteBuf buffer) {
    }

    public static LocateMasterStructureC2SPacket decode(FriendlyByteBuf buffer) {
        return new LocateMasterStructureC2SPacket();
    }

    public static void handle(LocateMasterStructureC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !hasScouter(player)) {
                return;
            }

            BlockPos nearest = locateNearestMasterStructure(player.serverLevel(), player.blockPosition());
            DmzRevampNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncMasterStructureS2CPacket(nearest));
        });
        context.setPacketHandled(true);
    }

    private static BlockPos locateNearestMasterStructure(ServerLevel level, BlockPos origin) {
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (ResourceLocation location : MASTER_STRUCTURES) {
            try {
                ResourceKey<Structure> key = ResourceKey.create(Registries.STRUCTURE, location);
                BlockPos found = StructureLocator.locateStructure(level, key, origin);
                if (found == null) {
                    continue;
                }
                double distance = origin.distSqr(found);
                if (distance < nearestDistance) {
                    nearest = found;
                    nearestDistance = distance;
                }
            } catch (Exception exception) {
                LOGGER.debug("Could not locate DMZ master structure {}: {}", location, exception.getMessage());
            }
        }
        return nearest;
    }

    private static boolean hasScouter(ServerPlayer player) {
        var stack = com.dragonminez.common.util.CuriosUtil.getFirstStack(player, "head_tech");
        return !stack.isEmpty() && stack.getItem().getDescriptionId().contains("scouter");
    }
}
