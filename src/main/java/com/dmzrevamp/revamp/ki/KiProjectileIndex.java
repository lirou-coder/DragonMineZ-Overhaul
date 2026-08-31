package com.dmzrevamp.revamp.ki;

import com.dmzrevamp.DmzRevampMod;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** Server-side index of DMZ Ki projectiles, maintained by entity lifecycle events. */
@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class KiProjectileIndex {
    private static final Map<ServerLevel, Set<AbstractKiProjectile>> BY_LEVEL = new WeakHashMap<>();

    private KiProjectileIndex() {
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level && event.getEntity() instanceof AbstractKiProjectile projectile) {
            synchronized (BY_LEVEL) {
                BY_LEVEL.computeIfAbsent(level, ignored -> Collections.newSetFromMap(new IdentityHashMap<>())).add(projectile);
            }
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level && event.getEntity() instanceof AbstractKiProjectile projectile) {
            synchronized (BY_LEVEL) {
                Set<AbstractKiProjectile> projectiles = BY_LEVEL.get(level);
                if (projectiles != null) {
                    projectiles.remove(projectile);
                    if (projectiles.isEmpty()) {
                        BY_LEVEL.remove(level);
                    }
                }
            }
        }
    }

    public static List<AbstractKiProjectile> snapshot(ServerLevel level) {
        synchronized (BY_LEVEL) {
            Set<AbstractKiProjectile> projectiles = BY_LEVEL.get(level);
            if (projectiles == null || projectiles.isEmpty()) {
                return List.of();
            }
            projectiles.removeIf(projectile -> projectile.isRemoved() || projectile.level() != level);
            if (projectiles.isEmpty()) {
                BY_LEVEL.remove(level);
                return List.of();
            }
            return new ArrayList<>(projectiles);
        }
    }
}
