package com.dmzrevamp.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "com.dragonminez.server.world.raid.RaidSiteManager$Kind", remap = false)
public interface RaidSiteKindAccessor {
    @Accessor("raidId")
    String dmzrevamp$getRaidId();
}
