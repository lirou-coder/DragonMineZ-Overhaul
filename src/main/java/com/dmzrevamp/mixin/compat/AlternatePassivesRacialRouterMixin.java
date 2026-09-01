package com.dmzrevamp.mixin.compat;

import com.dragonminez.common.stats.StatsData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Locale;

/**
 * Presents Overhaul racial skills to Alternate Passives as their DMZ base
 * counterparts without changing the racial skill stored by DMZ.
 */
@Pseudo
@Mixin(targets = "com.example.alternatepassives.race.RacialRouter", remap = false)
public abstract class AlternatePassivesRacialRouterMixin {

    @Inject(method = "racialSkillOf", at = @At("RETURN"), cancellable = true, require = 0)
    private static void dmzrevamp$normalizeRacialSkill(StatsData data, CallbackInfoReturnable<String> cir) {
        String racialSkill = cir.getReturnValue();
        if (racialSkill == null) {
            return;
        }

        String normalized = switch (racialSkill.toLowerCase(Locale.ROOT)) {
            case "saiyanrevamp" -> "saiyan";
            case "frostrevamp", "frostdemonrevamp" -> "frostdemon";
            case "humanrevamp" -> "human";
            case "bioandroidrevamp" -> "bioandroid";
            case "namekianrevamp" -> "namekian";
            case "majinrevamp" -> "majin";
            default -> racialSkill;
        };

        if (!normalized.equals(racialSkill)) {
            cir.setReturnValue(normalized);
        }
    }
}
