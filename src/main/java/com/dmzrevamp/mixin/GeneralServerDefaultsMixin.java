package com.dmzrevamp.mixin;

import com.dragonminez.common.config.GeneralServerConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

/**
 * Single source of truth for every Overhaul default written to general-server.json.
 * Gson applies values from an existing file after construction, so these values only
 * affect newly generated fields/files and never force an edited server configuration.
 */
@Mixin(value = GeneralServerConfig.class, remap = false)
public abstract class GeneralServerDefaultsMixin {
    @Unique
    private static final String[] DMZREVAMP_ALL_STATS = {"STR", "SKP", "PWR", "DEF", "STM"};

    @Inject(method = "<init>", at = @At("RETURN"))
    private void dmzrevamp$setGeneralServerDefaults(CallbackInfo ci) {
        GeneralServerConfig config = (GeneralServerConfig) (Object) this;

        GeneralServerConfig.GameplayConfig gameplay = config.getGameplay();
        dmzrevamp$set(gameplay, "maxValue", 1_000_000);
        dmzrevamp$set(gameplay, "fusionBoosts", DMZREVAMP_ALL_STATS.clone());
        dmzrevamp$set(gameplay, "multiplicationInsteadOfAdditionForMultipliers", true);

        GeneralServerConfig.RacialSkillsConfig racial = config.getRacialSkills();
        dmzrevamp$set(racial.getNamekian(), "assimilationBoosts", DMZREVAMP_ALL_STATS.clone());
        dmzrevamp$set(racial.getMajin(), "absorptionBoosts", DMZREVAMP_ALL_STATS.clone());
        dmzrevamp$set(racial.getFrostdemon(), "reserveFullChargeSeconds", 300);
        dmzrevamp$set(racial.getFrostdemon(), "reserveDurationSeconds", 120);
        dmzrevamp$set(racial.getFrostdemon(), "reserveFormBonus", 0.4D);

        GeneralServerConfig.GravityConfig gravity = config.getGravity();
        dmzrevamp$set(gravity, "resistanceStatDivisorRatio", 0.5D);
        dmzrevamp$set(gravity, "resistanceScale", 1000.0D);
        dmzrevamp$set(gravity, "tpIdealBaseDivisor", 1.0D);
        dmzrevamp$set(gravity, "tpPeakMultiplier", 3.0D);
    }

    @Unique
    private static void dmzrevamp$set(Object owner, String fieldName, Object value) {
        try {
            Field field = owner.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(owner, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to set Dragon Mine Z default '" + fieldName + "'", exception);
        }
    }
}
