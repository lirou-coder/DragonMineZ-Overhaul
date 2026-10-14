package com.dmzrevamp.mixin.client;

import com.dragonminez.client.gui.tournament.TournamentBracketScreen;
import com.dragonminez.common.network.TournamentPackets;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = TournamentBracketScreen.class, remap = false)
public interface TournamentBracketScreenAccessor {
    @Accessor("data")
    TournamentPackets.OpenBracketS2C dmzrevamp$getTournamentData();
}
