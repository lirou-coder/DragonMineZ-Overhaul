package com.dmzrevamp.revamp;

import com.dmzrevamp.DmzRevampMod;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Resources;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EnergyRegenPrecisionEvents {
    private EnergyRegenPrecisionEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void applyDecimalEnergyRegen(DMZEvent.EnergyRegenEvent event) {
        if (event.getPlayer().level().isClientSide() || event.isCanceled()) {
            return;
        }

        double amount = event.getAmount();
        if (!Double.isFinite(amount) || amount == 0.0D) {
            return;
        }

        StatsData data = event.getStatsData();
        Resources resources = data.getResources();
        float maxEnergy = data.getMaxEnergy();
        float effectiveMaxEnergy = maxEnergy;
        if (data.getStatus().hasActiveShadowDummy()) {
            effectiveMaxEnergy = maxEnergy * (1.0F - data.getStatus().getShadowDummyPercent() / 100.0F);
        }

        double nextEnergy = Math.max(0.0D, Math.min(effectiveMaxEnergy,
                resources.getCurrentEnergy() + amount));
        resources.setCurrentEnergy((float) nextEnergy);
        event.setCanceled(true);
    }
}
