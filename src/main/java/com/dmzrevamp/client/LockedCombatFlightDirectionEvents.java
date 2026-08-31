package com.dmzrevamp.client;

import com.dmzrevamp.DmzRevampMod;
import com.dmzrevamp.config.DmzRevampConfig;
import com.dmzrevamp.mixin.client.LockOnEventAccessor;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LockedCombatFlightDirectionEvents {
    private LockedCombatFlightDirectionEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void orientAfterDmzFlight(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!DmzRevampConfig.COMBAT_FLIGHT_TRIDIMENSIONAL.get()) return;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        var target = LockOnEventAccessor.dmzrevamp$getLockedTarget();
        if (player == null || target == null || !target.isAlive()) return;
        boolean forward = minecraft.options.keyUp.isDown();
        boolean backward = minecraft.options.keyDown.isDown();
        if (forward == backward) return;

        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            if (data.getStatus().getFlightMode() != 1 || !data.getSkills().isSkillActive("fly")) return;
            Vec3 motion = player.getDeltaMovement();
            double speed = motion.length();
            Vec3 direction = target.getBoundingBox().getCenter().subtract(player.getEyePosition());
            if (speed <= 1.0E-8D || direction.lengthSqr() <= 1.0E-8D) return;

            Vec3 lockedForward = direction.normalize();
            Vec3 horizontalForward = new Vec3(lockedForward.x, 0.0D, lockedForward.z);
            if (horizontalForward.lengthSqr() <= 1.0E-8D) {
                horizontalForward = Vec3.directionFromRotation(0.0F, player.getYRot()).normalize();
            } else {
                horizontalForward = horizontalForward.normalize();
            }
            Vec3 right = horizontalForward.cross(new Vec3(0.0D, 1.0D, 0.0D)).normalize();

            Vec3 input = lockedForward.scale(forward ? 1.0D : -1.0D);
            if (minecraft.options.keyLeft.isDown()) input = input.add(right.scale(-1.0D));
            if (minecraft.options.keyRight.isDown()) input = input.add(right);
            if (minecraft.options.keyJump.isDown()) input = input.add(0.0D, 1.0D, 0.0D);
            if (minecraft.options.keyShift.isDown()) input = input.add(0.0D, -1.0D, 0.0D);

            if (input.lengthSqr() > 1.0E-8D) {
                player.setDeltaMovement(input.normalize().scale(speed));
            }
        });
    }
}
