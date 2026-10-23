package com.dmzrevamp.mixin;

import com.dmzrevamp.config.FusionsRevampedConfig;
import com.dmzrevamp.revamp.cosmetic.FusionCosmeticArmorEvents;
import com.dmzrevamp.revamp.fusion.FusionRevampLogic;
import com.dmzrevamp.revamp.fusion.FusionRevampEvents;
import com.dmzrevamp.compat.NoeaCompat;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
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

    @Redirect(
            method = "executeMetamoru",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/server/util/FusionLogic;metamoruBlocker(Lcom/dragonminez/common/stats/StatsData;Lcom/dragonminez/common/stats/StatsData;)Ljava/lang/String;"),
            remap = false
    )
    private static String dmzrevamp$allowConfiguredMetamoruRules(StatsData leaderData, StatsData partnerData) {
        return FusionRevampLogic.configuredMetamoruBlocker(leaderData, partnerData);
    }

    @Inject(method = "calculateAndApplyStats", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dmzrevamp$applyRevampedFusionStats(StatsData data, StatsData otherData, String fusionType, int playerTotalStats, int otherTotalStats, CallbackInfo ci) {
        if (!FusionsRevampedConfig.isRevampedEnabled()) {
            return;
        }
        if (!NoeaCompat.overhaulOwnsStandardPlayerFusion(data, otherData)) {
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
