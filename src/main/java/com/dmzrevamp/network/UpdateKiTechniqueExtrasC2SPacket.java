package com.dmzrevamp.network;

import com.dmzrevamp.compat.DmzSkillProgressionCompat;
import com.dmzrevamp.compat.SkillProgressionTechniqueRandomizer;
import com.dmzrevamp.revamp.ki.KiAttackArchetype;
import com.dmzrevamp.revamp.ki.KiAttackExtraEffect;
import com.dmzrevamp.revamp.ki.RevampKiAttackData;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ProgressionSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.techniques.TechniqueData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Comparator;
import java.util.function.Supplier;

public record UpdateKiTechniqueExtrasC2SPacket(
        String techniqueName,
        String thirdType,
        String thirdStat,
        float thirdIntensity,
        int thirdDuration,
        String fourthType,
        String fourthStat,
        float fourthIntensity,
        int fourthDuration,
        String extraOneMode,
        String extraOneEffect,
        int extraOneLevel,
        int extraOneDuration,
        String extraTwoMode,
        String extraTwoEffect,
        int extraTwoLevel,
        int extraTwoDuration,
        String archetype,
        int multiCastCount,
        int domainDuration,
        boolean areaBothUtility,
        boolean continuous,
        float targetSize
) {
    public static void encode(UpdateKiTechniqueExtrasC2SPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.techniqueName);
        buffer.writeUtf(packet.thirdType);
        buffer.writeUtf(packet.thirdStat);
        buffer.writeFloat(packet.thirdIntensity);
        buffer.writeInt(packet.thirdDuration);
        buffer.writeUtf(packet.fourthType);
        buffer.writeUtf(packet.fourthStat);
        buffer.writeFloat(packet.fourthIntensity);
        buffer.writeInt(packet.fourthDuration);
        buffer.writeUtf(packet.extraOneMode);
        buffer.writeUtf(packet.extraOneEffect);
        buffer.writeInt(packet.extraOneLevel);
        buffer.writeInt(packet.extraOneDuration);
        buffer.writeUtf(packet.extraTwoMode);
        buffer.writeUtf(packet.extraTwoEffect);
        buffer.writeInt(packet.extraTwoLevel);
        buffer.writeInt(packet.extraTwoDuration);
        buffer.writeUtf(packet.archetype);
        buffer.writeInt(packet.multiCastCount);
        buffer.writeInt(packet.domainDuration);
        buffer.writeBoolean(packet.areaBothUtility);
        buffer.writeBoolean(packet.continuous);
        buffer.writeFloat(packet.targetSize);
    }

    public static UpdateKiTechniqueExtrasC2SPacket decode(FriendlyByteBuf buffer) {
        return new UpdateKiTechniqueExtrasC2SPacket(
                buffer.readUtf(),
                buffer.readUtf(),
                buffer.readUtf(),
                buffer.readFloat(),
                buffer.readInt(),
                buffer.readUtf(),
                buffer.readUtf(),
                buffer.readFloat(),
                buffer.readInt(),
                buffer.readUtf(),
                buffer.readUtf(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readUtf(),
                buffer.readUtf(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readUtf(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readFloat()
        );
    }

    public static void handle(UpdateKiTechniqueExtrasC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
                String author = player.getDisplayName().getString();
                KiAttackData target = data.getTechniques().getUnlockedTechniques().values().stream()
                        .filter(KiAttackData.class::isInstance)
                        .map(KiAttackData.class::cast)
                        .filter(technique -> packet.techniqueName.equals(technique.getName()))
                        .filter(technique -> author.equals(technique.getAuthor()))
                        .max(Comparator.comparing(TechniqueData::getId))
                        .orElse(null);

                if (!(target instanceof RevampKiAttackData revamp)) {
                    return;
                }

                if (DmzSkillProgressionCompat.isLoaded()) {
                    SkillProgressionTechniqueRandomizer.randomizeKi(player, target, revamp);
                    revamp.dmzrevamp$setAreaBothUtility(false);
                } else {
                    revamp.dmzrevamp$setThirdEffect(parseThirdType(packet.thirdType), parseStat(packet.thirdStat), packet.thirdIntensity, packet.thirdDuration);
                    revamp.dmzrevamp$setFourthEffect(parseThirdType(packet.fourthType), parseStat(packet.fourthStat), packet.fourthIntensity, packet.fourthDuration);
                    revamp.dmzrevamp$getExtraEffectOne().set(parseMode(packet.extraOneMode), packet.extraOneEffect, packet.extraOneLevel, packet.extraOneDuration);
                    revamp.dmzrevamp$getExtraEffectTwo().set(parseMode(packet.extraTwoMode), packet.extraTwoEffect, packet.extraTwoLevel, packet.extraTwoDuration);
                    revamp.dmzrevamp$setArchetype(parseArchetype(packet.archetype), packet.multiCastCount, packet.domainDuration);
                    revamp.dmzrevamp$setAreaBothUtility(packet.areaBothUtility);
                }
                revamp.dmzrevamp$setContinuous(false);
                target.calculateDerivedValues();
                NetworkHandler.sendToTrackingEntityAndSelf(new ProgressionSyncS2C(player), player);
            });
        });
        context.setPacketHandled(true);
    }

    private static KiAttackData.SecondaryEffectType parseThirdType(String value) {
        try {
            return KiAttackData.SecondaryEffectType.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return KiAttackData.SecondaryEffectType.NONE;
        }
    }

    private static KiAttackData.AffectedStat parseStat(String value) {
        try {
            return KiAttackData.AffectedStat.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return KiAttackData.AffectedStat.STR;
        }
    }

    private static KiAttackExtraEffect.Mode parseMode(String value) {
        try {
            return KiAttackExtraEffect.Mode.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return KiAttackExtraEffect.Mode.NONE;
        }
    }

    private static KiAttackArchetype parseArchetype(String value) {
        try {
            KiAttackArchetype archetype = KiAttackArchetype.valueOf(value);
            return archetype == KiAttackArchetype.MULTI_CAST ? KiAttackArchetype.NORMAL : archetype;
        } catch (IllegalArgumentException ignored) {
            return KiAttackArchetype.NORMAL;
        }
    }

}
