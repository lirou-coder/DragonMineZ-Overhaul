// Applies racial Ki-regeneration bonuses through DMZ's regen event instead of changing the base stat files.
package com.dmzrevamp.racial;

import com.dmzrevamp.DmzRevampMod;
import com.dmzrevamp.config.racial.DmzRevampRacialConfigs;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.stats.StatsData;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DmzRacialRegenEvents {
    // Forge calls the static event methods directly, so this event holder should not be instantiated.
    private DmzRacialRegenEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEnergyRegen(DMZEvent.EnergyRegenEvent event) {
        if (event.getPlayer().level().isClientSide() || event.getAmount() <= 0D) {
            return;
        }

        double regenBonus = getKiRegenBonus(event.getStatsData());
        if (regenBonus <= 0D) {
            return;
        }

        event.setAmount(event.getAmount() * (1D + regenBonus));
    }

    public static double getKiRegenBonus(StatsData data) {
        String racialId = CustomRacialActionHelper.getConfiguredRacialSkillId(data);
        if ("humanrevamp".equalsIgnoreCase(racialId)) {
            double bonus = DmzRevampRacialConfigs.humanRpg().kiRegenBonus;
            return data.getStatus().isAndroidUpgraded()
                    ? bonus * DmzRevampRacialConfigs.humanRpg().androidUpgradedKiRegenBonusMultiplier
                    : bonus;
        }
        if ("bioandroidrevamp".equalsIgnoreCase(racialId)) {
            return DmzRevampRacialConfigs.humanRpg().kiRegenBonus * DmzRevampRacialConfigs.bioAndroid().effectMultiplier;
        }
        return 0D;
    }
}
