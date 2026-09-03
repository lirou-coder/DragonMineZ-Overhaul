package com.dmzrevamp.mixin;

import com.dmzrevamp.config.FusionsRevampedConfig;
import com.dmzrevamp.revamp.cosmetic.FusionCosmeticArmorEvents;
import com.dmzrevamp.revamp.fusion.FusionRevampLogic;
import com.dmzrevamp.revamp.fusion.FusionRevampEvents;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Status;
import com.dragonminez.server.util.FusionLogic;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FusionLogic.class)
public abstract class FusionLogicCosmeticArmorMixin {
    private static final ThreadLocal<Boolean> DMZREVAMP_METAMORU_BOTH_ANDROIDS = ThreadLocal.withInitial(() -> false);

    @Redirect(
            method = "applyFusion",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/quest/PartyManager;beginFusionParty(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/server/level/ServerPlayer;)V"),
            remap = false
    )
    private static void dmzrevamp$doNotCreateArtificialFusionParty(ServerPlayer leader, ServerPlayer partner) {
        // Saga progress is party-owned. DMZ normally creates or merges a party
        // here solely because the players fused, exposing the leader's quests
        // and unclaimed rewards to a partner who was never in that party.
        // Real parties already exist and need no mutation during fusion.
    }

    @Inject(method = "endFusion", at = @At("HEAD"), remap = false)
    private static void dmzrevamp$clearFusionCosmeticArmor(ServerPlayer player, StatsData data, boolean forced, CallbackInfo ci) {
        if (player != null) {
            FusionCosmeticArmorEvents.clearFusionRow(player);
        }
        dmzrevamp$clearFusionBonusesForPair(player, data);
    }

    private static void dmzrevamp$clearFusionBonusesForPair(ServerPlayer player, StatsData data) {
        FusionRevampLogic.clearFusionBonuses(data);
        if (player == null || data == null || data.getStatus().getFusionPartnerUUID() == null) {
            return;
        }
        ServerPlayer partner = player.server.getPlayerList().getPlayer(data.getStatus().getFusionPartnerUUID());
        if (partner != null) {
            StatsProvider.get(StatsCapability.INSTANCE, partner).ifPresent(FusionRevampLogic::clearFusionBonuses);
        }
    }

    @Inject(method = "executeMetamoru", at = @At("HEAD"), remap = false)
    private static void dmzrevamp$rememberAndroidFusionPair(ServerPlayer player, ServerPlayer otherPlayer, StatsData data, StatsData otherData, CallbackInfoReturnable<Boolean> cir) {
        boolean bothAndroids = data != null && otherData != null
                && data.getStatus().isAndroidUpgraded()
                && otherData.getStatus().isAndroidUpgraded();
        DMZREVAMP_METAMORU_BOTH_ANDROIDS.set(bothAndroids);
    }

    @Inject(method = "executeMetamoru", at = @At("RETURN"), remap = false)
    private static void dmzrevamp$clearAndroidFusionPair(ServerPlayer player, ServerPlayer otherPlayer, StatsData data, StatsData otherData, CallbackInfoReturnable<Boolean> cir) {
        DMZREVAMP_METAMORU_BOTH_ANDROIDS.remove();
    }

    @Redirect(
            method = "executeMetamoru",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/stats/character/Status;isAndroidUpgraded()Z"),
            remap = false
    )
    private static boolean dmzrevamp$allowConfiguredAndroidFusion(Status status) {
        if (FusionsRevampedConfig.shouldBypassAndroidMetamoruCheck(DMZREVAMP_METAMORU_BOTH_ANDROIDS.get())) {
            return false;
        }
        return status.isAndroidUpgraded();
    }

    @Redirect(
            method = "executeMetamoru",
            at = @At(value = "INVOKE", target = "Ljava/lang/String;equals(Ljava/lang/Object;)Z"),
            remap = false
    )
    private static boolean dmzrevamp$allowDifferentRaceFusionDance(String raceName, Object otherRaceName) {
        return FusionsRevampedConfig.canUseDifferentRaceMetamoru() || raceName.equals(otherRaceName);
    }

    @Inject(method = "calculateAndApplyStats", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dmzrevamp$applyRevampedFusionStats(StatsData data, StatsData otherData, String fusionType, int playerTotalStats, int otherTotalStats, CallbackInfo ci) {
        if (!FusionsRevampedConfig.isRevampedEnabled()) {
            return;
        }
        FusionRevampLogic.applyFusionBonuses(data, otherData, fusionType, playerTotalStats, otherTotalStats);
        ci.cancel();
    }

    @Inject(method = "applyFusion", at = @At("HEAD"), remap = false)
    private static void dmzrevamp$captureResourcesBeforeFusionState(
            ServerPlayer leader, ServerPlayer partner, StatsData leaderData, StatsData partnerData,
            String fusionType, int leaderTotalStats, int partnerTotalStats, CallbackInfo ci) {
        if (FusionsRevampedConfig.isRevampedEnabled()) {
            // This must run before DMZ marks the pair as fused. Once isFused is
            // true, the revamp scale hooks already expose the fused Ki/Stamina
            // maxima and a full pre-fusion resource bar would look like 50%.
            FusionRevampEvents.captureAndScheduleResourceAverage(leaderData, partnerData);
        }
    }
}
