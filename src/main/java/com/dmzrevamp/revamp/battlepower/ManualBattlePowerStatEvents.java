package com.dmzrevamp.revamp.battlepower;

import com.dmzrevamp.DmzRevampMod;
import com.dragonminez.common.init.entities.IBattlePower;
import com.dragonminez.common.init.entities.MastersEntity;
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
    private ManualBattlePowerStatEvents() {
    }

    public static void syncDmzBattlePower(LivingEntity entity) {
        if (entity.level().isClientSide() || entity instanceof Player || !isDmzEntity(entity)) {
            return;
        }

        if (mustHideKi(entity)) {
            setBattlePower(entity, Integer.MAX_VALUE);
            return;
        }
        int currentBattlePower = currentBattlePower(entity);
        if (!canStoreBattlePower(entity)) {
            return;
        }
        if (currentBattlePower == Integer.MAX_VALUE) {
            return;
        }

        long calculatedBattlePower = AccurateMobBattlePowerCalculator.calculateCurvedBattlePower(entity);
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
        return currentBattlePower(entity) == Integer.MAX_VALUE;
    }

    public static long displayedBattlePower(LivingEntity entity, long fallback) {
        if (entity instanceof Player) {
            return fallback;
        }
        if (!isDmzEntity(entity) || isKiSenseHiddenEntity(entity)) {
            return fallback;
        }

        if (entity.level().isClientSide()) {
            int syncedBattlePower = currentBattlePower(entity);
            if (syncedBattlePower > 0) {
                return syncedBattlePower;
            }
            // Master NPCs do not store DMZ battle power, so clients calculate them from their live attributes.
            if (entity instanceof MastersEntity) {
                long calculated = AccurateMobBattlePowerCalculator.calculateCurvedBattlePower(entity);
                return calculated > 0L ? calculated : fallback;
            }
            return fallback;
        }

        long calculated = AccurateMobBattlePowerCalculator.calculateCurvedBattlePower(entity);
        return calculated > 0L ? calculated : fallback;
    }

    @SubscribeEvent
    public static void syncExistingDmzBattlePower(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.tickCount % 40 != 0) {
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
        return path.matches("a_\\d+") || name.matches("a_\\d+")
                || path.equals("saga_metal_cooler") || path.equals("saga_gete_robot")
                || name.equals("meta cooler") || name.equals("metal cooler") || name.equals("gete robot");
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
