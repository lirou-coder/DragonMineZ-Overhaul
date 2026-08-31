package com.dmzrevamp.mixin.compat.sdu;

import net.shurui.dev.sdu.client.gui.race.RaceClassStatsScreen;
import net.shurui.dev.sdu.race.RaceData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(value = RaceClassStatsScreen.class, remap = false)
public interface SduRaceClassStatsAccessor {
    @Accessor("className")
    String dmzrevamp$className();

    @Accessor("cs")
    RaceData.ClassStats dmzrevamp$classStats();

    @Accessor("section")
    int dmzrevamp$section();
}
