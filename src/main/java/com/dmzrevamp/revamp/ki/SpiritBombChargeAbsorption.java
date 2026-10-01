package com.dmzrevamp.revamp.ki;

import com.dmzrevamp.DmzRevampMod;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.dragonminez.common.init.entities.ki.KiExplosionEntity;
import com.dragonminez.common.init.entities.ki.KiBlastEntity;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.techniques.KiAttackData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/** Lets the native Spirit Bomb's physical charge sphere absorb allied Ki projectiles. */
@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SpiritBombChargeAbsorption {
    private static final String TECHNIQUE_ID = "spiritbomb";
    private static final String BASE_DAMAGE_TAG = DmzRevampMod.MODID + "_spirit_bomb_base_damage";
    private static final String BONUS_DAMAGE_TAG = DmzRevampMod.MODID + "_spirit_bomb_absorbed_damage";
    private static final String SIZE_MULTIPLIER_TAG = DmzRevampMod.MODID + "_spirit_bomb_absorbed_size";
    private static final String BONUS_APPLIED_TAG = DmzRevampMod.MODID + "_spirit_bomb_bonus_applied";
    private static final float CONTRIBUTION_RATIO = 0.5F;
    private static final float MAX_SIZE_MULTIPLIER = 4.0F;

    private SpiritBombChargeAbsorption() {}

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;

        List<AbstractKiProjectile> projectiles = KiProjectileIndex.snapshot(level);
        List<KiBlastEntity> chargingBombs = projectiles.stream()
                .filter(KiBlastEntity.class::isInstance)
                .map(KiBlastEntity.class::cast)
                .filter(SpiritBombChargeAbsorption::isChargingSpiritBomb)
                .toList();
        if (chargingBombs.isEmpty()) return;

        for (AbstractKiProjectile incoming : projectiles) {
            if (!isValidContribution(incoming)) continue;
            for (KiBlastEntity spiritBomb : chargingBombs) {
                if (incoming.isRemoved()) break;
                if (sameOwner(incoming, spiritBomb) || !intersectsChargeSphere(incoming, spiritBomb)) continue;
                absorb(spiritBomb, incoming);
                incoming.discard();
                break;
            }
        }
    }

    public static boolean isSpiritBomb(AbstractKiProjectile projectile) {
        return projectile instanceof KiBlastEntity && TECHNIQUE_ID.equals(projectile.getTechniqueId());
    }

    public static float sizeMultiplier(AbstractKiProjectile projectile) {
        if (!isSpiritBomb(projectile)) return 1.0F;
        float multiplier = projectile.getPersistentData().getFloat(SIZE_MULTIPLIER_TAG);
        return Float.isFinite(multiplier) && multiplier >= 1.0F
                ? Math.min(MAX_SIZE_MULTIPLIER, multiplier) : 1.0F;
    }

    public static float unboostedSize(AbstractKiProjectile projectile) {
        return Math.max(0.01F, projectile.getSize() / sizeMultiplier(projectile));
    }

    public static double withStoredDamageBonus(AbstractKiProjectile projectile, double ownDamage) {
        if (!isSpiritBomb(projectile)) return ownDamage;
        double bonus = projectile.getPersistentData().getDouble(BONUS_DAMAGE_TAG);
        if (!Double.isFinite(bonus) || bonus <= 0D) return ownDamage;
        return Math.min(Float.MAX_VALUE, Math.max(0D, ownDamage) + bonus);
    }

    /** Called after DMZ replaces the charging entity's damage with its final charge-scaled damage. */
    public static void applyStoredBonusOnFire(KiBlastEntity spiritBomb) {
        if (!isSpiritBomb(spiritBomb)) return;
        CompoundTag tag = spiritBomb.getPersistentData();
        if (tag.getBoolean(BONUS_APPLIED_TAG)) return;
        double finalDamage = withStoredDamageBonus(spiritBomb, spiritBomb.getKiDamage());
        spiritBomb.setKiDamage((float) finalDamage);
        tag.putBoolean(BONUS_APPLIED_TAG, true);
    }

    private static boolean isChargingSpiritBomb(KiBlastEntity projectile) {
        return !projectile.isRemoved() && !projectile.isFiring() && TECHNIQUE_ID.equals(projectile.getTechniqueId())
                && projectile.getOwner() instanceof Player;
    }

    private static boolean isValidContribution(AbstractKiProjectile projectile) {
        return !projectile.isRemoved() && projectile.isFiring() && !projectile.isHeal()
                && projectile.getOwner() instanceof Player
                && Float.isFinite(projectile.getKiDamage()) && projectile.getKiDamage() > 0F;
    }

    private static boolean sameOwner(AbstractKiProjectile first, AbstractKiProjectile second) {
        Entity firstOwner = first.getOwner();
        Entity secondOwner = second.getOwner();
        return firstOwner == null || secondOwner == null || firstOwner.getUUID().equals(secondOwner.getUUID());
    }

    private static void absorb(KiBlastEntity spiritBomb, AbstractKiProjectile incoming) {
        CompoundTag tag = spiritBomb.getPersistentData();
        double baseDamage = tag.contains(BASE_DAMAGE_TAG)
                ? tag.getDouble(BASE_DAMAGE_TAG) : resolveFullChargeBaseDamage(spiritBomb);
        baseDamage = finitePositive(baseDamage, Math.max(0.0001D, spiritBomb.getKiDamage() * 10D));
        tag.putDouble(BASE_DAMAGE_TAG, baseDamage);

        double contributionDamage = incoming.getKiDamage();
        double currentBonus = tag.getDouble(BONUS_DAMAGE_TAG);
        double newBonus = Math.min(Float.MAX_VALUE, Math.max(0D, currentBonus) + contributionDamage * CONTRIBUTION_RATIO);
        tag.putDouble(BONUS_DAMAGE_TAG, newBonus);

        float oldMultiplier = sizeMultiplier(spiritBomb);
        float ownSize = Math.max(0.01F, spiritBomb.getSize() / oldMultiplier);
        float sizeGain = (float) (CONTRIBUTION_RATIO * contributionDamage / baseDamage);
        float newMultiplier = Math.min(MAX_SIZE_MULTIPLIER, oldMultiplier + Math.max(0F, sizeGain));
        tag.putFloat(SIZE_MULTIPLIER_TAG, newMultiplier);
        spiritBomb.setSize(ownSize * newMultiplier);
    }

    private static double resolveFullChargeBaseDamage(KiBlastEntity spiritBomb) {
        if (!(spiritBomb.getOwner() instanceof Player owner)) return 0D;
        return StatsProvider.get(StatsCapability.INSTANCE, owner).map(data -> {
            Object technique = data.getTechniques().getUnlockedTechniques().get(TECHNIQUE_ID);
            if (!(technique instanceof KiAttackData attack)) return 0D;
            return data.getKiDamage() * attack.getDamageMultiplier()
                    * attack.getConfiguredDamageMultiplier() * attack.getOutputMultiplier();
        }).orElse(0D);
    }

    private static boolean intersectsChargeSphere(AbstractKiProjectile incoming, KiBlastEntity spiritBomb) {
        Vec3 center = new Vec3(spiritBomb.getX(), spiritBomb.getY() + spiritBomb.getSize() * 0.5D, spiritBomb.getZ());
        double spiritRadius = Math.max(0.1D, spiritBomb.getSize() * 0.5D);

        Vec3 start;
        Vec3 end;
        double incomingRadius;
        if (incoming instanceof KiExplosionEntity explosion) {
            start = incoming.position().add(0D, incoming.getBbHeight() * 0.5D, 0D);
            end = start;
            incomingRadius = Math.max(0.1D, explosion.getMaxRadius());
        } else if (incoming.getClashBeamLength() > 0.1F) {
            start = incoming.position();
            Vec3 direction = Vec3.directionFromRotation(incoming.getClashPitch(), incoming.getClashYaw());
            end = start.add(direction.scale(incoming.getClashBeamLength()));
            incomingRadius = Math.max(0.1D, incoming.getSize() * 0.5D);
        } else {
            end = incoming.position().add(0D, incoming.getBbHeight() * 0.5D, 0D);
            start = end.subtract(incoming.getDeltaMovement());
            incomingRadius = Math.max(0.1D, Math.max(incoming.getBbWidth(), incoming.getBbHeight()) * 0.5D);
        }
        double radius = spiritRadius + incomingRadius;
        return pointSegmentDistanceSqr(center, start, end) <= radius * radius;
    }

    private static double pointSegmentDistanceSqr(Vec3 point, Vec3 start, Vec3 end) {
        Vec3 segment = end.subtract(start);
        double lengthSqr = segment.lengthSqr();
        if (lengthSqr <= 1.0E-9D) return point.distanceToSqr(start);
        double position = Math.max(0D, Math.min(1D, point.subtract(start).dot(segment) / lengthSqr));
        return point.distanceToSqr(start.add(segment.scale(position)));
    }

    private static double finitePositive(double value, double fallback) {
        return Double.isFinite(value) && value > 0D ? value : fallback;
    }
}
