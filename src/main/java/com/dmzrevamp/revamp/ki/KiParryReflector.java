package com.dmzrevamp.revamp.ki;

import com.dmzrevamp.mixin.KiLaserEntityAccessor;
import com.dmzrevamp.mixin.KiWaveEntityAccessor;
import com.dmzrevamp.revamp.classes.skills.ClassSkillHelper;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.dragonminez.common.init.entities.ki.KiBlastEntity;
import com.dragonminez.common.init.entities.ki.KiDiskEntity;
import com.dragonminez.common.init.entities.ki.KiLaserEntity;
import com.dragonminez.common.init.entities.ki.KiWaveEntity;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile.KiType;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

/** Server-side reflection of the actual projectile involved in a confirmed DMZ parry. */
public final class KiParryReflector {
    private static final int REPEAT_REFLECT_TICKS = 5;
    private static final Map<AbstractKiProjectile, Long> LAST_REFLECT_TICK = new WeakHashMap<>();
    private static final Map<KiBlastEntity, Boolean> REFLECTED_BLAST_IMPACTS = new WeakHashMap<>();

    private KiParryReflector() {
    }

    public static boolean reflectIfSupported(AbstractKiProjectile projectile, ServerPlayer parrier) {
        if (projectile.level().isClientSide || projectile.isRemoved() || parrier == null) return false;
        if (!isReflectable(projectile)) return false;

        long now = projectile.level().getGameTime();
        Long previous = LAST_REFLECT_TICK.get(projectile);
        boolean barrage = isBarrage(projectile);
        if (previous != null && now - previous < REPEAT_REFLECT_TICKS) return false;
        LAST_REFLECT_TICK.put(projectile, now);

        Vec3 direction = directionFor(parrier);
        projectile.setOwner(parrier);
        projectile.setHomingTarget(-1);
        projectile.clearClashLock();
        float yaw = (float) (Math.atan2(direction.z, direction.x) * (180D / Math.PI)) - 90F;
        float pitch = (float) (-(Math.atan2(direction.y, Math.sqrt(direction.x * direction.x + direction.z * direction.z))
                * (180D / Math.PI)));
        projectile.setYRot(yaw);
        projectile.setXRot(pitch);

        if (projectile instanceof KiBlastEntity || projectile instanceof KiDiskEntity) {
            double speed = projectile.getDeltaMovement().length();
            if (speed < 0.2D) speed = Math.max(0.2D, projectile.getKiSpeed());
            projectile.setDeltaMovement(direction.scale(speed));
            projectile.hurtMarked = true;
            projectile.hasImpulse = true;
            positionOutsidePlayer(projectile, parrier, direction);
            if (projectile instanceof KiBlastEntity blast) REFLECTED_BLAST_IMPACTS.put(blast, Boolean.TRUE);
            if (barrage) resetDmzParryWindow(parrier);
            return true;
        }

        if (projectile instanceof KiWaveEntity wave) {
            wave.getEntityData().set(KiWaveEntityAccessor.dmzrevamp$getFixedYawKey(), yaw);
            wave.getEntityData().set(KiWaveEntityAccessor.dmzrevamp$getFixedPitchKey(), pitch);
            wave.setContinuousFollow(false);
            wave.setBeamLength(0F);
            positionBeamOrigin(wave, parrier, direction);
        } else if (projectile instanceof KiLaserEntity laser) {
            laser.getEntityData().set(KiLaserEntityAccessor.dmzrevamp$getFixedYawKey(), yaw);
            laser.getEntityData().set(KiLaserEntityAccessor.dmzrevamp$getFixedPitchKey(), pitch);
            laser.setBeamLength(0F);
            positionBeamOrigin(laser, parrier, direction);
        }
        projectile.hurtMarked = true;
        return true;
    }

    /** Called after KiBlastEntity's current hit has returned from damage processing. */
    public static boolean consumeReflectedBlastImpact(KiBlastEntity blast) {
        return REFLECTED_BLAST_IMPACTS.remove(blast) != null;
    }

    private static boolean isReflectable(AbstractKiProjectile projectile) {
        KiType type = projectile.getKiType();
        if (type == KiType.EXPLOSION || type == KiType.AREA || type == KiType.SHIELD) return false;
        if (projectile instanceof KiBlastEntity) {
            return type == KiType.SMALL_BALL || type == KiType.MEDIUM_BALL
                    || type == KiType.GIANT_BALL || type == KiType.BARRAGE;
        }
        if (projectile instanceof KiDiskEntity) return type == KiType.DISK && projectile.getDeltaMovement().lengthSqr() > 1.0E-8D;
        // The concrete DMZ wave/laser entity has a fixed trajectory even when a
        // technique labels its variant (for example chain-shot) with another KiType.
        return projectile instanceof KiWaveEntity || projectile instanceof KiLaserEntity;
    }

    private static boolean isBarrage(AbstractKiProjectile projectile) {
        return projectile instanceof KiBlastEntity && projectile.getKiType() == KiType.BARRAGE;
    }

    /** DMZ evaluates parry eligibility from Status.lastBlockTime + parryWindowMs. */
    private static void resetDmzParryWindow(ServerPlayer player) {
        StatsProvider.get(StatsCapability.INSTANCE, player).resolve()
                .ifPresent(stats -> stats.getStatus().setLastBlockTime(System.currentTimeMillis()));
    }

    private static Vec3 directionFor(ServerPlayer player) {
        Vec3 look = player.getLookAngle().normalize();
        StatsData stats = StatsProvider.get(StatsCapability.INSTANCE, player).resolve().orElse(null);
        if (stats != null && ClassSkillHelper.hasClassPassive(stats, ClassSkillHelper.DUELIST)) return look;

        // Uniform solid-angle sampling within a 100-degree half-angle cone (200 degrees total).
        double minCos = Math.cos(Math.toRadians(100D));
        double cosTheta = minCos + player.getRandom().nextDouble() * (1D - minCos);
        double sinTheta = Math.sqrt(Math.max(0D, 1D - cosTheta * cosTheta));
        double phi = player.getRandom().nextDouble() * (Math.PI * 2D);
        Vec3 reference = Math.abs(look.y) < 0.9D ? new Vec3(0D, 1D, 0D) : new Vec3(1D, 0D, 0D);
        Vec3 right = look.cross(reference).normalize();
        Vec3 up = right.cross(look).normalize();
        return look.scale(cosTheta)
                .add(right.scale(sinTheta * Math.cos(phi)))
                .add(up.scale(sinTheta * Math.sin(phi)))
                .normalize();
    }

    private static void positionOutsidePlayer(AbstractKiProjectile projectile, ServerPlayer player, Vec3 direction) {
        double clearance = player.getBbWidth() * 0.5D + projectile.getBbWidth() * 0.5D + 0.15D;
        Vec3 position = player.position().add(0D, player.getBbHeight() * 0.5D, 0D).add(direction.scale(clearance));
        projectile.setPos(position.x, position.y, position.z);
    }

    private static void positionBeamOrigin(AbstractKiProjectile projectile, ServerPlayer player, Vec3 direction) {
        double clearance = player.getBbWidth() * 0.5D + 0.25D;
        Vec3 position = player.getEyePosition().add(direction.scale(clearance));
        projectile.setPos(position.x, position.y, position.z);
    }
}
