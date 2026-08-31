package com.dmzrevamp.network;

import com.dmzrevamp.DmzRevampMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class DmzRevampNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(DmzRevampMod.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;
    private static boolean registered = false;

    // This class only owns the shared packet channel and should never be created as an object.
    private DmzRevampNetwork() {
    }

    // Gives every client/server packet a stable id so Forge can encode it, decode it, and run its handler on the other side.
    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        CHANNEL.registerMessage(packetId++, UpdateKiTechniqueExtrasC2SPacket.class, UpdateKiTechniqueExtrasC2SPacket::encode, UpdateKiTechniqueExtrasC2SPacket::decode, UpdateKiTechniqueExtrasC2SPacket::handle);
        CHANNEL.registerMessage(packetId++, CreateStrikeTechniqueC2SPacket.class, CreateStrikeTechniqueC2SPacket::encode, CreateStrikeTechniqueC2SPacket::decode, CreateStrikeTechniqueC2SPacket::handle);
        CHANNEL.registerMessage(packetId++, InternalCosmeticArmorSyncS2CPacket.class, InternalCosmeticArmorSyncS2CPacket::encode, InternalCosmeticArmorSyncS2CPacket::decode, InternalCosmeticArmorSyncS2CPacket::handle);
        CHANNEL.registerMessage(packetId++, OverchargeScreenShakeS2CPacket.class, OverchargeScreenShakeS2CPacket::encode, OverchargeScreenShakeS2CPacket::decode, OverchargeScreenShakeS2CPacket::handle);
        CHANNEL.registerMessage(packetId++, LocateMasterStructureC2SPacket.class, LocateMasterStructureC2SPacket::encode, LocateMasterStructureC2SPacket::decode, LocateMasterStructureC2SPacket::handle);
        CHANNEL.registerMessage(packetId++, SyncMasterStructureS2CPacket.class, SyncMasterStructureS2CPacket::encode, SyncMasterStructureS2CPacket::decode, SyncMasterStructureS2CPacket::handle);
        CHANNEL.registerMessage(packetId++, ClashTransformChargeC2SPacket.class, ClashTransformChargeC2SPacket::encode, ClashTransformChargeC2SPacket::decode, ClashTransformChargeC2SPacket::handle);
        CHANNEL.registerMessage(packetId++, PrestigeC2SPacket.class, PrestigeC2SPacket::encode, PrestigeC2SPacket::decode, PrestigeC2SPacket::handle);
        CHANNEL.registerMessage(packetId++, PrestigeFusionFlashS2CPacket.class, PrestigeFusionFlashS2CPacket::encode, PrestigeFusionFlashS2CPacket::decode, PrestigeFusionFlashS2CPacket::handle);
        CHANNEL.registerMessage(packetId++, CombatFlightDashImpulseS2CPacket.class, CombatFlightDashImpulseS2CPacket::encode, CombatFlightDashImpulseS2CPacket::decode, CombatFlightDashImpulseS2CPacket::handle);
        CHANNEL.registerMessage(packetId++, StrikeYAnchorS2CPacket.class, StrikeYAnchorS2CPacket::encode, StrikeYAnchorS2CPacket::decode, StrikeYAnchorS2CPacket::handle);
        CHANNEL.registerMessage(packetId++, StrikeClashModeS2CPacket.class, StrikeClashModeS2CPacket::encode, StrikeClashModeS2CPacket::decode, StrikeClashModeS2CPacket::handle);
        CHANNEL.registerMessage(packetId++, StrikeClashInputC2SPacket.class, StrikeClashInputC2SPacket::encode, StrikeClashInputC2SPacket::decode, StrikeClashInputC2SPacket::handle);
        CHANNEL.registerMessage(packetId++, RequestSduClassEditorC2SPacket.class, RequestSduClassEditorC2SPacket::encode, RequestSduClassEditorC2SPacket::decode, RequestSduClassEditorC2SPacket::handle);
        CHANNEL.registerMessage(packetId++, OpenSduClassEditorS2CPacket.class, OpenSduClassEditorS2CPacket::encode, OpenSduClassEditorS2CPacket::decode, OpenSduClassEditorS2CPacket::handle);
        CHANNEL.registerMessage(packetId++, SaveSduClassC2SPacket.class, SaveSduClassC2SPacket::encode, SaveSduClassC2SPacket::decode, SaveSduClassC2SPacket::handle);
        CHANNEL.registerMessage(packetId++, SetSpeedLimitC2SPacket.class, SetSpeedLimitC2SPacket::encode, SetSpeedLimitC2SPacket::decode, SetSpeedLimitC2SPacket::handle);
    }
}
