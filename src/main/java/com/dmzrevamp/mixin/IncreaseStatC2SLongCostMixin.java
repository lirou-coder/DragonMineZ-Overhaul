package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.stats.LongTpCostHelper;
import com.dragonminez.common.network.C2S.IncreaseStatC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.server.events.players.StatsEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;
import java.util.function.Supplier;

@Mixin(value = IncreaseStatC2S.class, remap = false)
public abstract class IncreaseStatC2SLongCostMixin {
    @Shadow
    @Final
    private IncreaseStatC2S.StatType statType;

    @Shadow
    @Final
    private int multiplier;

    @Inject(method = "handle", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dmzrevamp$handleLongTpCost(IncreaseStatC2S message, Supplier<NetworkEvent.Context> contextSupplier, CallbackInfo ci) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(statsData -> {
                IncreaseStatC2SLongCostMixin accessor = (IncreaseStatC2SLongCostMixin) (Object) message;
                dmzrevamp$increaseStatSafely(statsData, player, accessor.statType.name(), accessor.multiplier);
            });
        });
        context.setPacketHandled(true);
        ci.cancel();
    }

    private static void dmzrevamp$increaseStatSafely(StatsData statsData, ServerPlayer player, String statName, int requestedIncrease) {
        int pendingPoints = statsData.getResources().getPendingAttributePoints();
        float trainingPoints = statsData.getResources().getTrainingPoints();
        if (pendingPoints <= 0 && trainingPoints <= 0F) {
            return;
        }

        int allowedIncrease = statsData.getMaxAllowedIncreaseForStat(statName, requestedIncrease);
        if (allowedIncrease <= 0) {
            return;
        }

        boolean changed = false;
        int pendingIncrease = Math.min(pendingPoints, allowedIncrease);
        if (pendingIncrease > 0) {
            dmzrevamp$increaseStat(statsData, player, statName, pendingIncrease);
            statsData.getResources().removePendingAttributePoints(pendingIncrease);
            changed = true;
        }

        int remainingIncrease = allowedIncrease - pendingIncrease;
        if (remainingIncrease > 0 && trainingPoints > 0F) {
            int allowedTpIncrease = statsData.getMaxAllowedIncreaseForStat(statName, remainingIncrease);
            int purchasedIncrease = LongTpCostHelper.calculateAffordableIncrease(statsData, allowedTpIncrease, trainingPoints);
            long cost = LongTpCostHelper.calculateRecursiveCost(statsData, purchasedIncrease);
            if (purchasedIncrease > 0 && cost > 0L && cost <= (double) trainingPoints) {
                dmzrevamp$increaseStat(statsData, player, statName, purchasedIncrease);
                statsData.getResources().removeTrainingPoints((float) cost);
                changed = true;
            }
        }

        if (changed) {
            NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
        }
    }

    private static void dmzrevamp$increaseStat(StatsData statsData, ServerPlayer player, String statName, int amount) {
        switch (statName.toUpperCase(Locale.ROOT)) {
            case "STR" -> statsData.getStats().addStrength(amount);
            case "SKP" -> statsData.getStats().addStrikePower(amount);
            case "RES" -> {
                float oldStamina = statsData.getMaxStamina();
                statsData.getStats().addResistance(amount);
                float newStamina = statsData.getMaxStamina();
                if (newStamina > oldStamina) {
                    statsData.getResources().addStamina(newStamina - oldStamina);
                }
            }
            case "VIT" -> {
                float oldHealthBonus = statsData.getHealthBonus();
                statsData.getStats().addVitality(amount);
                float newHealthBonus = statsData.getHealthBonus();
                float addedHealth = newHealthBonus - oldHealthBonus;
                if (addedHealth > 0F) {
                    AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
                    if (maxHealth != null) {
                        maxHealth.removeModifier(StatsEvents.DMZ_HEALTH_MODIFIER_UUID);
                        maxHealth.addPermanentModifier(new AttributeModifier(
                                StatsEvents.DMZ_HEALTH_MODIFIER_UUID,
                                "DMZ Health",
                                newHealthBonus,
                                AttributeModifier.Operation.ADDITION
                        ));
                    }
                    player.heal(addedHealth);
                }
            }
            case "PWR" -> statsData.getStats().addKiPower(amount);
            case "ENE" -> {
                float oldEnergy = statsData.getMaxEnergy();
                statsData.getStats().addEnergy(amount);
                float newEnergy = statsData.getMaxEnergy();
                if (newEnergy > oldEnergy) {
                    statsData.getResources().addEnergy(newEnergy - oldEnergy);
                }
            }
            default -> {
            }
        }
    }
}
