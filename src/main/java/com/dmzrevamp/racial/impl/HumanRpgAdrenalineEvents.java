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

import java.util.List;
import java.util.Locale;

@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HumanRpgAdrenalineEvents {
    private static final String HUMAN_BONUS_KEY = "Ki Boosting Body";
    private static final String BIO_BONUS_KEY = "Perfect DNA Ki Boost";

    private HumanRpgAdrenalineEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide() || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        if ((player.tickCount % 10) != 0) {
            return;
        }

        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> updatePowerBoost(player, data));
    }

    private static void updatePowerBoost(ServerPlayer player, StatsData data) {
        String racialId = CustomRacialActionHelper.getConfiguredRacialSkillId(data);
        boolean human = "humanrevamp".equalsIgnoreCase(racialId);
        boolean bioAndroid = "bioandroidrevamp".equalsIgnoreCase(racialId);
        if (!human && !bioAndroid) {
            removeBoosts(data);
            return;
        }

        double maxEnergy = Math.max(1D, data.getMaxEnergy());
        double currentRatio = data.getResources().getCurrentEnergy() / maxEnergy;
        if (currentRatio + 0.0001D < DmzRevampRacialConfigs.humanRpg().fullKiThreshold) {
            removeBoosts(data);
            return;
        }

        double boost = DmzRevampRacialConfigs.humanRpg().fullKiPowerBoost;
        String bonusKey = HUMAN_BONUS_KEY;
        if (human && data.getStatus().isAndroidUpgraded()) {
            boost *= DmzRevampRacialConfigs.humanRpg().androidUpgradedFullKiPowerBoostMultiplier;
        } else if (bioAndroid) {
            boost *= DmzRevampRacialConfigs.bioAndroid().effectMultiplier;
            bonusKey = BIO_BONUS_KEY;
        }

        boolean changed = false;
        String otherBonusKey = HUMAN_BONUS_KEY.equals(bonusKey) ? BIO_BONUS_KEY : HUMAN_BONUS_KEY;
        if (data.getBonusStats().hasBonus("STR", otherBonusKey)
                || data.getBonusStats().hasBonus("SKP", otherBonusKey)
                || data.getBonusStats().hasBonus("PWR", otherBonusKey)
                || data.getBonusStats().hasBonus("DEF", otherBonusKey)
                || data.getBonusStats().hasBonus("RES", otherBonusKey)) {
            data.getBonusStats().removeAllBonuses(otherBonusKey);
            changed = true;
        }

        for (String stat : DmzRevampRacialConfigs.humanRpg().boostedStats) {
            String normalizedStat = normalizeHumanBoostStat(stat);
            double multiplier = 1.0D + boost;
            if (Math.abs(getNamedBonusValue(data, normalizedStat, bonusKey) - multiplier) > 0.0001D) {
                data.getBonusStats().removeBonus(normalizedStat, bonusKey);
                data.getBonusStats().addBonus(normalizedStat, bonusKey, "*", multiplier);
                if ("DEF".equals(normalizedStat)) {
                    data.getBonusStats().removeBonus("RES", bonusKey);
                    data.getBonusStats().removeBonus("STM", bonusKey);
                }
                changed = true;
            }
        }
        if (changed) {
            NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
        }
    }

    private static void removeBoosts(StatsData data) {
        removeIfPresent(data, HUMAN_BONUS_KEY, DmzRevampRacialConfigs.humanRpg().boostedStats);
        removeIfPresent(data, BIO_BONUS_KEY, DmzRevampRacialConfigs.humanRpg().boostedStats);
    }

    private static void removeIfPresent(StatsData data, String bonusKey, List<String> stats) {
        for (String stat : stats) {
            data.getBonusStats().removeBonus(normalizeHumanBoostStat(stat), bonusKey);
        }
        data.getBonusStats().removeBonus("RES", bonusKey);
        data.getBonusStats().removeBonus("STM", bonusKey);
    }

    private static double getNamedBonusValue(StatsData data, String stat, String bonusKey) {
        return data.getBonusStats().getBonuses(stat).stream()
                .filter(bonus -> bonusKey.equals(bonus.name))
                .mapToDouble(bonus -> bonus.value)
                .findFirst()
                .orElse(0D);
    }

    private static String normalizeHumanBoostStat(String stat) {
        String normalized = stat == null ? "" : stat.toUpperCase(Locale.ROOT);
        return "RES".equals(normalized) ? "DEF" : normalized;
    }
}
