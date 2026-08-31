package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.prestige.PrestigeDifficultyHelper;
import com.dmzrevamp.revamp.prestige.PrestigeSystem;
import com.dragonminez.common.quest.Difficulty;
import com.dragonminez.server.events.EntitiesEvents;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = EntitiesEvents.class, remap = false)
public abstract class EntitiesEventsPrestigeDifficultyMixin {
    @Redirect(
            method = "onEntityJoinWorld",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/quest/Difficulty;hpMultiplier()D"),
            require = 0
    )
    private static double dmzrevamp$combinePrestigeHealthDifficulty(Difficulty difficulty, EntityJoinLevelEvent event) {
        return PrestigeSystem.roundedDifficultyValue(
                difficulty.hpMultiplier() * PrestigeDifficultyHelper.statMultiplier(event.getEntity()));
    }

    @Redirect(
            method = "onEntityJoinWorld",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/quest/Difficulty;damageMultiplier()D"),
            require = 0
    )
    private static double dmzrevamp$combinePrestigeDamageDifficulty(Difficulty difficulty, EntityJoinLevelEvent event) {
        return PrestigeSystem.roundedDifficultyValue(
                difficulty.damageMultiplier() * PrestigeDifficultyHelper.statMultiplier(event.getEntity()));
    }
}
