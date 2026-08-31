package com.dmzrevamp.racial.impl;

import com.dmzrevamp.DmzRevampMod;
import com.dmzrevamp.config.racial.DmzRevampRacialConfigs;
import com.dmzrevamp.racial.CustomRacialActionHelper;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Locale;

@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FrostDemonRevampEvents {
    public static final String BONUS_KEY = "Dangerously Fast";

    private FrostDemonRevampEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide() || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        if ((player.tickCount % 10) != 0) {
            return;
        }

        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> updateBoost(player, data));
    }

    private static void updateBoost(ServerPlayer player, StatsData data) {
        String racialId = CustomRacialActionHelper.getConfiguredRacialSkillId(data);
        boolean frost = "frostrevamp".equalsIgnoreCase(racialId);
        boolean bioAndroid = "bioandroidrevamp".equalsIgnoreCase(racialId);
        if (!frost && !bioAndroid) {
            removeBoost(data);
            return;
        }

        double missingHpPercent = Math.max(0D, 1D - (player.getHealth() / Math.max(0.0001D, player.getMaxHealth()))) * 100D;
        double boost = missingHpPercent * DmzRevampRacialConfigs.frostDemon().speedAndPowerBoostPerMissingHpPercent;
        if (bioAndroid) {
            boost *= DmzRevampRacialConfigs.bioAndroid().effectMultiplier;
        }
        if (boost <= 0D) {
            removeBoost(data);
            return;
        }

        boolean changed = false;
        for (String stat : DmzRevampRacialConfigs.frostDemon().boostedStats) {
            String normalizedStat = stat.toUpperCase(Locale.ROOT);
            double multiplier = 1.0D + boost;
            if (Math.abs(getNamedBonusValue(data, normalizedStat) - multiplier) > 0.0001D) {
                data.getBonusStats().removeBonus(normalizedStat, BONUS_KEY);
                data.getBonusStats().addBonus(normalizedStat, BONUS_KEY, "*", multiplier);
                changed = true;
            }
        }
        if (changed) {
            NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
        }
    }

    private static void removeBoost(StatsData data) {
        for (String stat : DmzRevampRacialConfigs.frostDemon().boostedStats) {
            data.getBonusStats().removeBonus(stat.toUpperCase(Locale.ROOT), BONUS_KEY);
        }
    }

    private static double getNamedBonusValue(StatsData data, String stat) {
        return data.getBonusStats().getBonuses(stat).stream()
                .filter(bonus -> BONUS_KEY.equals(bonus.name))
                .mapToDouble(bonus -> bonus.value)
                .findFirst()
                .orElse(0D);
    }

    public static float adjustAttackStaminaCost(StatsData data, float originalCost) {
        String racialId = CustomRacialActionHelper.getConfiguredRacialSkillId(data);
        boolean frost = "frostrevamp".equalsIgnoreCase(racialId);
        boolean bioAndroid = "bioandroidrevamp".equalsIgnoreCase(racialId);
        if (!frost && !bioAndroid) {
            return originalCost;
        }

        double reduction = DmzRevampRacialConfigs.frostDemon().attackStaminaCostReduction;
        if (bioAndroid) {
            reduction *= DmzRevampRacialConfigs.bioAndroid().effectMultiplier;
        }
        double multiplier = Math.max(0D, 1D - reduction);
        return (float) Math.max(0D, originalCost * multiplier);
    }
}
