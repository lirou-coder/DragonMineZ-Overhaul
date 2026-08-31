package com.dmzrevamp.client;

import com.dmzrevamp.mixin.client.CombatFlightHandlerStateAccessor;
import com.dragonminez.client.flight.CombatFlightHandler;
import com.dragonminez.common.init.EntityAttributes;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.phys.Vec3;

public final class CombatFlightDoubleDashImpulseClientEvents {
    private static final int FLIGHT_COMBAT = 1;

    private CombatFlightDoubleDashImpulseClientEvents() {
    }

    /** Called only by the server after DashHandler has actually completed a combat-flight dash. */
    public static void applyServerDashImpulse(int directionId) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }

        StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).resolve().orElse(null);
        if (!isCombatFlying(data)) {
            return;
        }
        applyImpulse(player, data, directionId);
    }

    private static boolean isCombatFlying(StatsData data) {
        return data != null
                && data.getStatus().isHasCreatedCharacter()
                && data.getSkills().isSkillActive("fly")
                && data.getStatus().getFlightMode() == FLIGHT_COMBAT;
    }

    private static void applyImpulse(LocalPlayer player, StatsData data, int directionId) {
        Vec3 direction = getImpulseVector(player, directionId);
        double distance = getImpulseDistance(player, data);
        if (direction.lengthSqr() <= 1.0E-6D || distance <= 0D) {
            return;
        }

        CombatFlightHandler.injectKnockback(direction.normalize().scale(distance));
        CombatFlightHandlerStateAccessor.dmzrevamp$setSustainedDir(directionId);
        player.fallDistance = 0F;
    }

    private static Vec3 getImpulseVector(LocalPlayer player, int directionId) {
        Vec3 forward = Vec3.directionFromRotation(0F, player.getYRot()).normalize();
        Vec3 right = forward.cross(new Vec3(0D, 1D, 0D)).normalize();
        return switch (directionId) {
            case 1 -> forward.scale(-1D);
            case 2 -> right.scale(-1D);
            case 3 -> right;
            case 4 -> new Vec3(0D, 1D, 0D);
            case 5 -> new Vec3(0D, -1D, 0D);
            default -> forward;
        };
    }

    private static double getImpulseDistance(LocalPlayer player, StatsData data) {
        int flyLevel = Math.max(0, data.getSkills().getSkillLevel("fly"));
        AttributeInstance flySpeedAttribute = player.getAttribute(EntityAttributes.FLY_SPEED.get());
        double flySpeed = flySpeedAttribute == null ? 0D : flySpeedAttribute.getValue();
        double flySpeedScale = flySpeed <= 0D ? 1D : Mth.clamp(flySpeed / 0.35D, 0.25D, 4D);
        return flySpeedScale * (1D + 0.2D * flyLevel);
    }

}
