package com.dmzrevamp.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.UUID;

@Mixin(targets = "com.dragonminez.server.events.players.combat.StrikeAttackHandler$ActiveStrike", remap = false)
public interface StrikeAttackActiveAccessor {
    @Accessor("targetId")
    UUID dmzrevamp$getTargetId();

    @Accessor("techniqueId")
    String dmzrevamp$getTechniqueId();

    @Accessor("durationTicks")
    int dmzrevamp$getDurationTicks();

    @Accessor("animationId")
    String dmzrevamp$getAnimationId();

    @Accessor("cooldownTicks")
    int dmzrevamp$getCooldownTicks();

    @Accessor("totalDamage")
    double dmzrevamp$getTotalDamage();

    @Accessor("perHitDamage")
    double dmzrevamp$getPerHitDamage();

    @Accessor("ticksElapsed")
    int dmzrevamp$getTicksElapsed();
}
