package com.dmzrevamp.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.UUID;

@Mixin(targets = "com.dragonminez.server.events.players.combat.StrikeAttackHandler$PendingStrike", remap = false)
public interface StrikeAttackPendingAccessor {
    @Accessor("techniqueId")
    String dmzrevamp$getTechniqueId();

    @Accessor("preferredTargetId")
    UUID dmzrevamp$getPreferredTargetId();
}
