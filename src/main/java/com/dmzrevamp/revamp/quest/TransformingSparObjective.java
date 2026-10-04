package com.dmzrevamp.revamp.quest;

import com.dragonminez.common.quest.objectives.KillObjective;
import com.dragonminez.common.quest.objectives.SparObjective;

/**
 * Temporary compatibility objective for DMZ 2.2 builds whose SparObjective
 * constructor discards the native KILL transformation values.
 */
public final class TransformingSparObjective extends SparObjective {
    private final Double transformHealth;
    private final Double transformMeleeDamage;
    private final Double transformKiDamage;
    private final Double transformHealthMultiplier;
    private final Double transformMeleeMultiplier;
    private final Double transformKiMultiplier;
    private final Double transformTriggerPercent;

    public TransformingSparObjective(
            String entityId,
            int count,
            double health,
            double meleeDamage,
            double kiDamage,
            KillObjective.SpawnMode spawnMode,
            KillObjective.CountMode countMode,
            int textureVariant,
            int aiTier,
            boolean canTransform,
            Double transformHealth,
            Double transformMeleeDamage,
            Double transformKiDamage,
            Double transformHealthMultiplier,
            Double transformMeleeMultiplier,
            Double transformKiMultiplier,
            Double transformTriggerPercent
    ) {
        super(entityId, count, health, meleeDamage, kiDamage, spawnMode, countMode,
                textureVariant, aiTier, canTransform);
        this.transformHealth = transformHealth;
        this.transformMeleeDamage = transformMeleeDamage;
        this.transformKiDamage = transformKiDamage;
        this.transformHealthMultiplier = transformHealthMultiplier;
        this.transformMeleeMultiplier = transformMeleeMultiplier;
        this.transformKiMultiplier = transformKiMultiplier;
        this.transformTriggerPercent = transformTriggerPercent;
    }

    @Override public Double getTransformHealth() { return transformHealth; }
    @Override public Double getTransformMeleeDamage() { return transformMeleeDamage; }
    @Override public Double getTransformKiDamage() { return transformKiDamage; }
    @Override public Double getTransformHealthMultiplier() { return transformHealthMultiplier; }
    @Override public Double getTransformMeleeMultiplier() { return transformMeleeMultiplier; }
    @Override public Double getTransformKiMultiplier() { return transformKiMultiplier; }
    @Override public Double getTransformTriggerPercent() { return transformTriggerPercent; }
}
