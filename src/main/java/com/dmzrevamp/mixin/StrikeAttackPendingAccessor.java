package com.dmzrevamp.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "com.dragonminez.server.events.players.combat.StrikeAttackHandler$PendingStrike", remap = false)
public interface StrikeAttackPendingAccessor {
    @Accessor("techniqueId")
    String dmzrevamp$getTechniqueId();
}
