package com.dmzrevamp.mixin;

import com.dmzrevamp.config.LevelingRevampConfig;
import com.dmzrevamp.revamp.prestige.PrestigeSystem;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Character;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Character.class)
public abstract class CharacterPrestigeMasteryMixin {
    @ModifyVariable(method = "gainMastery", at = @At("HEAD"), argsOnly = true, ordinal = 0, remap = false, require = 0)
    private double dmzrevamp$applyPrestigeMasteryGain(double amount) {
        if (!LevelingRevampConfig.prestigeEnabled()) return amount;
        // Character does not retain its owner, so resolve the matching live capability.
        MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return amount;
        for (Player player : server.getPlayerList().getPlayers()) {
            StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
            if (data != null && data.getCharacter() == (Object) this) {
                return amount * PrestigeSystem.masteryMultiplier(data);
            }
        }
        return amount;
    }
}
