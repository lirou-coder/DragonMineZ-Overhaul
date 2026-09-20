package com.dmzrevamp.revamp.battlepower;

import com.dmzrevamp.DmzRevampMod;
import com.dmzrevamp.config.KiSenseBlacklistConfig;
import com.dragonminez.common.init.entities.IBattlePower;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ManualBattlePowerStatEvents {
    private static final String CACHED_BP_TAG = "dmzrevamp_cached_battle_power";
    private ManualBattlePowerStatEvents() {
    }

    public static void syncDmzBattlePower(LivingEntity entity) {
        if (entity.level().isClientSide() || entity instanceof Player || KiSenseBlacklistConfig.contains(entity)) {
            return;
        }

        if (mustHideKi(entity)) {
            setBattlePower(entity, Integer.MAX_VALUE);
            return;
        }
        double exactBattlePower = AccurateMobBattlePowerCalculator.calculateCurvedBattlePowerExact(entity);
        entity.getPersistentData().putDouble(CACHED_BP_TAG, exactBattlePower);
        long calculatedBattlePower = exactBattlePower >= Long.MAX_VALUE ? Long.MAX_VALUE : (long) exactBattlePower;
        int storedBattlePower = AccurateMobBattlePowerCalculator.toStoredVisibleBattlePower(calculatedBattlePower);
        if (entity instanceof IBattlePower battlePower) {
            battlePower.setBattlePower(storedBattlePower);
        } else if (entity instanceof DBSagasEntity saga) {
            saga.setBattlePower(storedBattlePower);
        } else {
            return;
        }
    }

    public static boolean isAndroidKiHiddenEntity(LivingEntity entity) {
        return isKiSenseHiddenEntity(entity);
    }

    public static boolean isKiSenseHiddenEntity(LivingEntity entity) {
        return mustHideKi(entity);
    }

    public static long displayedBattlePower(LivingEntity entity, long fallback) {
        if (entity instanceof Player) {
            return fallback;
        }
        if (isKiSenseHiddenEntity(entity)) {
            return fallback;
        }

        if (entity.getPersistentData().contains(CACHED_BP_TAG)) {
            double cached = entity.getPersistentData().getDouble(CACHED_BP_TAG);
            if (Double.isFinite(cached) && cached > 0D) return Math.min(Long.MAX_VALUE, (long) cached);
        }

        if (entity.level().isClientSide()) {
            int syncedBattlePower = currentBattlePower(entity);
            if (syncedBattlePower > 0) {
                return syncedBattlePower;
            }
            return fallback;
        }
        return fallback;
    }

    public static int cachedStoredBattlePower(LivingEntity entity) {
        if (entity == null || !entity.getPersistentData().contains(CACHED_BP_TAG)) return 0;
        double cached = entity.getPersistentData().getDouble(CACHED_BP_TAG);
        if (!Double.isFinite(cached) || cached <= 0D) return 0;
        long value = cached >= Long.MAX_VALUE ? Long.MAX_VALUE : (long) cached;
        return AccurateMobBattlePowerCalculator.toStoredVisibleBattlePower(value);
    }

    public static void clearBattlePower(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide()) return;
        entity.getPersistentData().remove(CACHED_BP_TAG);
        setBattlePower(entity, 0);
    }

    @SubscribeEvent
    public static void syncExistingDmzBattlePower(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player || entity.tickCount != 6) {
            return;
        }
        syncDmzBattlePower(entity);
    }

    private static boolean isDmzEntity(LivingEntity entity) {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return key != null && ("dragonminez".equals(key.getNamespace()) || DmzRevampMod.MODID.equals(key.getNamespace()));
    }

    private static boolean canStoreBattlePower(LivingEntity entity) {
        return entity instanceof IBattlePower || entity instanceof DBSagasEntity;
    }

    private static boolean mustHideKi(LivingEntity entity) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        String path = id == null ? "" : id.getPath();
        String name = entity.getName().getString().trim().toLowerCase(java.util.Locale.ROOT);
        return path.matches("saga_(?:super_)?a\\d+")
                || path.equals("saga_gete_robot") || path.equals("saga_metal_cooler")
                || path.equals("saga_metal_cooler_core") || path.equals("saga_drgero")
                || name.equals("a13") || name.equals("android 13")
                || name.equals("a14") || name.equals("android 14")
                || name.equals("a15") || name.equals("android 15")
                || name.equals("super a13") || name.equals("super android 13")
                || name.equals("meta cooler") || name.equals("metal cooler")
                || name.equals("meta cooler core") || name.equals("metal cooler core")
                || name.equals("gete robot");
    }

    private static void setBattlePower(LivingEntity entity, int value) {
        if (entity instanceof IBattlePower battlePower) battlePower.setBattlePower(value);
        else if (entity instanceof DBSagasEntity saga) saga.setBattlePower(value);
    }

    private static int currentBattlePower(LivingEntity entity) {
        if (entity instanceof IBattlePower battlePower) {
            return battlePower.getBattlePower();
        }
        if (entity instanceof DBSagasEntity saga) {
            return saga.getBattlePower();
        }
        return 0;
    }
}
