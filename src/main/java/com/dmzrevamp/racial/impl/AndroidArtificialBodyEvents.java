// Gives upgraded Human Androids an artificial body: full food, no sprint exhaustion, and no vanilla food healing.
package com.dmzrevamp.racial.impl;

import com.dmzrevamp.DmzRevampMod;
import com.dmzrevamp.racial.CustomRacialActionHelper;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AndroidArtificialBodyEvents {
    private static final Map<UUID, Integer> INTERNAL_HEAL_GUARD = new ConcurrentHashMap<>();

    // Forge calls the static event methods directly, so this event holder should not be instantiated.
    private AndroidArtificialBodyEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    // Keeps Android-like players fed every tick so hunger no longer drives their survival loop.
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide() || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            if (!isAndroidLike(data)) {
                INTERNAL_HEAL_GUARD.remove(player.getUUID());
                return;
            }

            maintainFood(player);
        });
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    // Restores artificial-body food state as soon as an Android-like player joins.
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
                if (isAndroidLike(data)) {
                    maintainFood(player);
                }
            });
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    // Restores artificial-body food state after Minecraft creates a fresh player entity.
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
                if (isAndroidLike(data)) {
                    maintainFood(player);
                }
            });
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    // Drops short-lived guard data when the player leaves the world.
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            INTERNAL_HEAL_GUARD.remove(player.getUUID());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    // Blocks the small automatic heals vanilla gives from high food, while allowing real regeneration effects.
    public static void onLivingHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            if (!isAndroidLike(data)) {
                return;
            }
            int guardTick = INTERNAL_HEAL_GUARD.getOrDefault(player.getUUID(), Integer.MIN_VALUE);
            if (guardTick == player.tickCount) {
                return;
            }
            FoodData foodData = player.getFoodData();
            if (foodData.getFoodLevel() >= 18 && event.getAmount() <= 1.0F && !player.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION)) {
                event.setCanceled(true);
            }
        });
    }

    // Treats only upgraded Human-revamp players as artificial-body Androids.
    private static boolean isAndroidLike(StatsData data) {
        String racialId = CustomRacialActionHelper.getConfiguredRacialSkillId(data);
        return "humanrevamp".equalsIgnoreCase(racialId) && data.getStatus().isAndroidUpgraded();
    }

    // Sets food values to the stable Android baseline and removes accumulated hunger exhaustion.
    private static void maintainFood(ServerPlayer player) {
        FoodData foodData = player.getFoodData();
        foodData.setFoodLevel(20);
        foodData.setSaturation(Math.max(foodData.getSaturationLevel(), 5F));
        if (foodData.getExhaustionLevel() > 0F) {
            foodData.setExhaustion(0F);
        }
    }
}
