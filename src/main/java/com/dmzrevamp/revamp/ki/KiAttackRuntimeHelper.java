package com.dmzrevamp.revamp.ki;

import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.dragonminez.common.combat.logic.player.TargetHelper;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.techniques.TechniqueData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class KiAttackRuntimeHelper {
    private KiAttackRuntimeHelper() {
    }

    public static KiAttackData technique(AbstractKiProjectile projectile) {
        if (!(projectile.getOwner() instanceof ServerPlayer owner)) {
            return null;
        }
        StatsData ownerData = StatsProvider.get(StatsCapability.INSTANCE, owner).resolve().orElse(null);
        if (ownerData == null) {
            return null;
        }
        TechniqueData technique = ownerData.getTechniques().getUnlockedTechniques().get(projectile.getTechniqueId());
        return technique instanceof KiAttackData ki ? ki : null;
    }

    public static RevampKiAttackData revamp(AbstractKiProjectile projectile) {
        KiAttackData technique = technique(projectile);
        return technique instanceof RevampKiAttackData revamp ? revamp : null;
    }

    public static KiAttackArchetype archetype(AbstractKiProjectile projectile) {
        RevampKiAttackData revamp = revamp(projectile);
        return revamp == null ? KiAttackArchetype.NORMAL : revamp.dmzrevamp$getArchetype();
    }

    public static LivingEntity findLookTarget(AbstractKiProjectile projectile, double range, double maxAngleCos) {
        if (!(projectile.getOwner() instanceof LivingEntity owner)) {
            return null;
        }
        return findLookTarget(owner, range, maxAngleCos);
    }

    public static LivingEntity findLookTarget(LivingEntity owner, double range, double maxAngleCos) {
        Vec3 eye = owner.getEyePosition();
        Vec3 look = owner.getLookAngle().normalize();
        AABB box = owner.getBoundingBox().inflate(range);
        LivingEntity best = null;
        double bestDistance = range * range;
        for (LivingEntity candidate : owner.level().getEntitiesOfClass(LivingEntity.class, box, entity -> entity.isAlive() && entity != owner)) {
            Vec3 toTarget = candidate.getEyePosition().subtract(eye);
            double distance = toTarget.lengthSqr();
            if (distance <= 0.01D || distance > bestDistance) {
                continue;
            }
            double alignment = toTarget.normalize().dot(look);
            if (alignment >= maxAngleCos) {
                best = candidate;
                bestDistance = distance;
            }
        }
        return best;
    }

    public static LivingEntity findLineOfSightTarget(LivingEntity owner, double range) {
        Vec3 eye = owner.getEyePosition();
        Vec3 look = owner.getLookAngle().normalize();
        Vec3 end = eye.add(look.scale(range));
        AABB box = owner.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0D);
        LivingEntity best = null;
        double bestDistance = range * range;
        for (LivingEntity candidate : owner.level().getEntitiesOfClass(LivingEntity.class, box, entity -> entity.isAlive() && entity != owner)) {
            if (!owner.hasLineOfSight(candidate)) {
                continue;
            }
            if (candidate.getBoundingBox().inflate(0.35D).clip(eye, end).isEmpty()) {
                continue;
            }
            double distance = candidate.distanceToSqr(owner);
            if (distance < bestDistance) {
                best = candidate;
                bestDistance = distance;
            }
        }
        return best;
    }

    public static boolean isOwner(AbstractKiProjectile projectile, Entity target) {
        Entity owner = projectile.getOwner();
        return owner != null && owner.getUUID().equals(target.getUUID());
    }

    public static boolean isFriendlyToOwner(AbstractKiProjectile projectile, Entity target) {
        Entity owner = projectile.getOwner();
        if (owner == target) {
            return true;
        }
        if (owner instanceof Player playerOwner) {
            return TargetHelper.getRelation(playerOwner, target) == TargetHelper.Relation.FRIENDLY;
        }
        return false;
    }
}
