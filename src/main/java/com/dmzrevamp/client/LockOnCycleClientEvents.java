package com.dmzrevamp.client;

import com.dmzrevamp.DmzRevampMod;
import com.dmzrevamp.config.DmzRevampConfig;
import com.dmzrevamp.mixin.client.LockOnEventAccessor;
import com.dragonminez.client.events.LockOnEvent;
import com.dragonminez.client.systems.kisense.KiSenseScan;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

public final class LockOnCycleClientEvents {
    private static final int HISTORY_LIMIT = 3;
    private static final Deque<UUID> RECENT_TARGETS = new ArrayDeque<>(HISTORY_LIMIT);
    private static final KeyMapping LOCK_CYCLE = new KeyMapping(
            "key.dmzrevamp.lock_cycle",
            InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_MIDDLE,
            "key.categories.dmzrevamp"
    );

    private LockOnCycleClientEvents() {}

    @Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() {}

        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(LOCK_CYCLE);
        }
    }

    @Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class InputHandler {
        private InputHandler() {}

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            while (LOCK_CYCLE.consumeClick()) cycleTarget();
        }
    }

    private static void cycleTarget() {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        LivingEntity current = LockOnEventAccessor.dmzrevamp$getLockedTarget();
        if (player == null || current == null || !current.isAlive()) return;

        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            int level = data.getSkills().getSkillLevel("kisense");
            if (level <= 0 || data.getSkills().getSkill("kisense") == null) return;
            double range = 15.0D + 5.0D * level;
            if (data.getStatus().isAndroidUpgraded()) range += 25.0D;

            rememberTarget(current);
            List<LivingEntity> candidates = findPrioritizedTargets(player, range, data);
            LivingEntity next = firstNotRemembered(candidates, current);
            if (next == null) {
                clearHistory();
                rememberTarget(current);
                next = firstNotRemembered(candidates, current);
            }
            if (next != null) {
                rememberTarget(next);
                LockOnEventAccessor.dmzrevamp$setLockedTarget(next);
                player.playSound(MainSounds.LOCKON.get(), 1.0F, 1.0F);
            }
        });
    }

    private static LivingEntity firstNotRemembered(List<LivingEntity> candidates, LivingEntity current) {
        for (LivingEntity candidate : candidates) {
            if (candidate != current && !RECENT_TARGETS.contains(candidate.getUUID())) return candidate;
        }
        return null;
    }

    public static List<LivingEntity> findPrioritizedTargets(Player player, double range, StatsData data) {
        Vec3 eye = player.getEyePosition();
        Vec3 view = player.getViewVector(1.0F).normalize();
        List<TargetCandidate> candidates = new ArrayList<>();
        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(range),
                entity -> entity != player && entity.isAlive() && entity.isPickable() && canTarget(entity, data))) {
            if (target.isInvisible() || target.isInvisibleTo(player) || !player.hasLineOfSight(target)) continue;
            Vec3 direction = target.getBoundingBox().getCenter().subtract(eye);
            double distance = direction.length();
            if (distance <= 0.0D || distance > range) continue;
            int area = lockArea(view.dot(direction.scale(1.0D / distance)));
            if (area != Integer.MAX_VALUE) candidates.add(new TargetCandidate(target, area, distance));
        }
        candidates.sort(Comparator.comparingInt(TargetCandidate::area).thenComparingDouble(TargetCandidate::distance));
        return candidates.stream().map(TargetCandidate::entity).toList();
    }

    public static void rememberTarget(LivingEntity target) {
        UUID id = target.getUUID();
        RECENT_TARGETS.remove(id);
        RECENT_TARGETS.addLast(id);
        while (RECENT_TARGETS.size() > HISTORY_LIMIT) RECENT_TARGETS.removeFirst();
    }

    public static void clearHistory() {
        RECENT_TARGETS.clear();
    }

    public static boolean canTarget(LivingEntity target, StatsData data) {
        return DmzRevampConfig.ALLOW_LOCK_ON_ANDROID.get()
                ? LockOnEvent.canTarget(target, data)
                : KiSenseScan.canTarget(target, data);
    }

    private static int lockArea(double dot) {
        if (dot >= Math.cos(Math.toRadians(1.0D))) return 0;
        if (dot >= Math.cos(Math.toRadians(5.0D))) return 1;
        if (dot >= Math.cos(Math.toRadians(10.0D))) return 2;
        if (dot >= Math.cos(Math.toRadians(15.0D))) return 3;
        if (dot >= Math.cos(Math.toRadians(30.0D))) return 4;
        return Integer.MAX_VALUE;
    }

    private record TargetCandidate(LivingEntity entity, int area, double distance) {}
}
