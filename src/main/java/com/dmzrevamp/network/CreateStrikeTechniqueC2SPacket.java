package com.dmzrevamp.network;

import com.dmzrevamp.compat.DmzSkillProgressionCompat;
import com.dmzrevamp.compat.SkillProgressionTechniqueRandomizer;
import com.dmzrevamp.revamp.ki.KiAttackExtraEffect;
import com.dmzrevamp.revamp.strike.CustomStrikeType;
import com.dmzrevamp.revamp.strike.RevampStrikeAttackData;
import com.dmzrevamp.revamp.strike.StrikeAttackCategoryRules;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ProgressionSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.techniques.StrikeAttackData;
import com.dragonminez.common.stats.techniques.TechniqueData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record CreateStrikeTechniqueC2SPacket(
        String name,
        String strikeType,
        float damageMultiplier,
        float speedMultiplier,
        int armorPenetration,
        String secondaryType,
        String secondaryStat,
        float secondaryIntensity,
        int secondaryDuration,
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
        int extraTwoDuration
) {
    private static final int STRIKE_BASE_COOLDOWN_TICKS = 240;
    private static final int EVASIVE_BASE_COOLDOWN_TICKS = 400;

    public static void encode(CreateStrikeTechniqueC2SPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.name);
        buffer.writeUtf(packet.strikeType);
        buffer.writeFloat(packet.damageMultiplier);
        buffer.writeFloat(packet.speedMultiplier);
        buffer.writeInt(packet.armorPenetration);
        buffer.writeUtf(packet.secondaryType);
        buffer.writeUtf(packet.secondaryStat);
        buffer.writeFloat(packet.secondaryIntensity);
        buffer.writeInt(packet.secondaryDuration);
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
    }

    public static CreateStrikeTechniqueC2SPacket decode(FriendlyByteBuf buffer) {
        return new CreateStrikeTechniqueC2SPacket(
                buffer.readUtf(),
                buffer.readUtf(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readInt(),
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
                buffer.readFloat(),
                buffer.readInt(),
                buffer.readUtf(),
                buffer.readUtf(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readUtf(),
                buffer.readUtf(),
                buffer.readInt(),
                buffer.readInt()
        );
    }

    public static void handle(CreateStrikeTechniqueC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
                if (!data.getStatus().isHasCreatedCharacter()) {
                    return;
                }
                String cleanName = cleanName(packet.name);
                StrikeAttackData strike = new StrikeAttackData();
                strike.setName(cleanName);
                strike.setAuthor(player.getDisplayName().getString());
                strike.setId(TechniqueData.generateId(strike.getAuthor(), cleanName));
                if (data.getTechniques().getUnlockedTechniques().containsKey(strike.getId())) {
                    NetworkHandler.sendToTrackingEntityAndSelf(new ProgressionSyncS2C(player), player);
                    return;
                }

                CustomStrikeType type = CustomStrikeType.parse(packet.strikeType);
                float damage = Mth.clamp(packet.damageMultiplier, type.minDamageMultiplier(), type.maxDamageMultiplier());
                strike.setDamageMultiplier(damage);
                strike.setAnimationId(type.animationId());
                strike.setDurationTicks(type.durationTicks());
                strike.applyConfigDefaults();

                if (strike instanceof RevampStrikeAttackData revamp) {
                    revamp.dmzrevamp$setCustomStrike(true);
                    revamp.dmzrevamp$setStrikeType(type);
                    revamp.dmzrevamp$setDashSpeedMultiplier(packet.speedMultiplier);
                    revamp.dmzrevamp$setArmorPenetration(type.isEvasive() ? 0 : Mth.clamp(packet.armorPenetration, 0, 10));
                    if (DmzSkillProgressionCompat.isLoaded()) {
                        SkillProgressionTechniqueRandomizer.randomizeStrike(player, type, revamp);
                    } else {
                        revamp.dmzrevamp$setSecondaryEffect(filterSecondary(type, parseSecondary(packet.secondaryType)), parseStat(packet.secondaryStat), packet.secondaryIntensity, packet.secondaryDuration);
                        revamp.dmzrevamp$setThirdEffect(filterSecondary(type, parseSecondary(packet.thirdType)), parseStat(packet.thirdStat), packet.thirdIntensity, packet.thirdDuration);
                        revamp.dmzrevamp$setFourthEffect(filterSecondary(type, parseSecondary(packet.fourthType)), parseStat(packet.fourthStat), packet.fourthIntensity, packet.fourthDuration);
                        revamp.dmzrevamp$getExtraEffectOne().set(filterMode(type, parseMode(packet.extraOneMode)), packet.extraOneEffect, packet.extraOneLevel, packet.extraOneDuration);
                        revamp.dmzrevamp$getExtraEffectTwo().set(filterMode(type, parseMode(packet.extraTwoMode)), packet.extraTwoEffect, packet.extraTwoLevel, packet.extraTwoDuration);
                    }
                    strike.setCooldown(creationCooldown(strike, revamp));
                    strike.setTpCost(creationTpCost(strike, revamp));
                } else {
                    strike.setTpCost(100.0F);
                }

                int tpCost = Math.max(100, Math.round(strike.getTpCost()));
                if (data.getResources().getTrainingPoints() < tpCost) {
                    NetworkHandler.sendToTrackingEntityAndSelf(new ProgressionSyncS2C(player), player);
                    return;
                }
                data.getResources().removeTrainingPoints(tpCost);
                data.getTechniques().unlockTechnique(strike);
                NetworkHandler.sendToTrackingEntityAndSelf(new ProgressionSyncS2C(player), player);
            });
        });
        context.setPacketHandled(true);
    }

    private static float creationTpCost(StrikeAttackData strike, RevampStrikeAttackData revamp) {
        float typeRatio = strike.getDamageMultiplier() / Math.max(0.1F, revamp.dmzrevamp$getStrikeType().minDamageMultiplier());
        float speedRatio = 1.0F + Math.max(0.0F, revamp.dmzrevamp$getDashSpeedMultiplier() - 1.0F) * 0.35F;
        return Math.max(100.0F, 100.0F * typeRatio * speedRatio * revamp.dmzrevamp$getExtraCostMultiplier());
    }

    private static int creationCooldown(StrikeAttackData strike, RevampStrikeAttackData revamp) {
        float defaultDamage = Math.max(0.1F, revamp.dmzrevamp$getStrikeType().defaultDamageMultiplier());
        float damageRatio = Math.max(0.1F, strike.getDamageMultiplier()) / defaultDamage;
        float speedRatio = revamp.dmzrevamp$getStrikeType().isEvasive() ? 1.0F : 1.0F + Math.max(0.0F, revamp.dmzrevamp$getDashSpeedMultiplier() - 1.0F) * 0.35F;
        int baseCooldown = revamp.dmzrevamp$getStrikeType().isEvasive() ? EVASIVE_BASE_COOLDOWN_TICKS : STRIKE_BASE_COOLDOWN_TICKS;
        // The server repeats the creator math so packet tampering cannot bypass costs.
        return Math.max(1, Math.round((baseCooldown * damageRatio * speedRatio + revamp.dmzrevamp$getExtraCooldownTicks()) * revamp.dmzrevamp$getStrikeType().cooldownMultiplier()));
    }

    private static String cleanName(String raw) {
        String clean = raw == null || raw.trim().isEmpty() ? "New Strike" : raw.trim();
        return clean.length() > 64 ? clean.substring(0, 64) : clean;
    }

    private static KiAttackData.SecondaryEffectType filterSecondary(CustomStrikeType type, KiAttackData.SecondaryEffectType effectType) {
        if (type.isEvasive()) {
            return effectType == KiAttackData.SecondaryEffectType.BUFF ? effectType : KiAttackData.SecondaryEffectType.NONE;
        }
        return effectType == KiAttackData.SecondaryEffectType.DEBUFF ? effectType : KiAttackData.SecondaryEffectType.NONE;
    }

    private static KiAttackExtraEffect.Mode filterMode(CustomStrikeType type, KiAttackExtraEffect.Mode mode) {
        if (type.isEvasive()) {
            return mode == KiAttackExtraEffect.Mode.BENEFICIAL ? mode : KiAttackExtraEffect.Mode.NONE;
        }
        return mode == KiAttackExtraEffect.Mode.HARMFUL ? mode : KiAttackExtraEffect.Mode.NONE;
    }

    private static KiAttackData.SecondaryEffectType parseSecondary(String value) {
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
}
