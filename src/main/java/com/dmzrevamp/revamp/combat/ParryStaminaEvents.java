package com.dmzrevamp.revamp.combat;

import com.dmzrevamp.DmzRevampMod;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ParryStaminaEvents {
    private static final String PRE_PARRY_STAMINA_TAG = DmzRevampMod.MODID + "_pre_parry_stamina";

    private ParryStaminaEvents() {
    }

    public static void rememberPreParryStamina(ServerPlayer player, StatsData data) {
        player.getPersistentData().putFloat(PRE_PARRY_STAMINA_TAG, data.getResources().getCurrentStamina());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerBlock(DMZEvent.PlayerBlockEvent event) {
        if (!event.isParry()) {
            return;
        }

        ServerPlayer player = event.getVictim();
        if (!player.getPersistentData().contains(PRE_PARRY_STAMINA_TAG)) {
            return;
        }

        float preParryStamina = player.getPersistentData().getFloat(PRE_PARRY_STAMINA_TAG);
        player.getPersistentData().remove(PRE_PARRY_STAMINA_TAG);
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data ->
                data.getResources().setCurrentStamina(Math.min(data.getMaxStamina(), preParryStamina)));
    }
}
