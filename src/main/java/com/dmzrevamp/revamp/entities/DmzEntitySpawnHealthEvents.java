package com.dmzrevamp.revamp.entities;

import com.dmzrevamp.DmzRevampMod;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DmzEntitySpawnHealthEvents {
    private static final List<PendingHeal> PENDING_HEALS = new ArrayList<>();

    private DmzEntitySpawnHealthEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDmzEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof LivingEntity entity) || entity instanceof Player) {
            return;
        }

        EntityConfigAttributeApplier.apply(entity);
        PENDING_HEALS.add(new PendingHeal(level.dimension(), entity.getId(), 5));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        processPendingSpawnFinalization(event);
    }

    private static void processPendingSpawnFinalization(TickEvent.ServerTickEvent event) {
        if (PENDING_HEALS.isEmpty()) {
            return;
        }

        Iterator<PendingHeal> iterator = PENDING_HEALS.iterator();
        while (iterator.hasNext()) {
            PendingHeal pending = iterator.next();
            if (pending.delayTicks > 0) {
                pending.delayTicks--;
                continue;
            }

            ServerLevel level = event.getServer().getLevel(pending.level);
            if (level != null && level.getEntity(pending.entityId) instanceof LivingEntity entity && !entity.isDeadOrDying()) {
                EntityConfigAttributeApplier.apply(entity);

                boolean dmzEntity = isDmzEntity(entity);
                if (dmzEntity) {
                    healToFull(entity);
                }
            }

            pending.remainingPasses--;
            if (pending.remainingPasses <= 0) {
                iterator.remove();
            } else {
                pending.delayTicks = 1;
            }
        }
    }

    private static void healToFull(LivingEntity entity) {
        float maxHealth = entity.getMaxHealth();
        if (Float.isFinite(maxHealth) && maxHealth > 0F && entity.getHealth() < maxHealth) {
            entity.setHealth(maxHealth);
        }
    }

    private static boolean isDmzEntity(LivingEntity entity) {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return key != null && ("dragonminez".equals(key.getNamespace()) || DmzRevampMod.MODID.equals(key.getNamespace()));
    }

    private static final class PendingHeal {
        private final ResourceKey<Level> level;
        private final int entityId;
        private int delayTicks;
        private int remainingPasses;

        private PendingHeal(ResourceKey<Level> level, int entityId, int remainingPasses) {
            this.level = level;
            this.entityId = entityId;
            this.delayTicks = 2;
            this.remainingPasses = remainingPasses;
        }
    }
}
