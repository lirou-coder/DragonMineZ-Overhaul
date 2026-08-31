package com.dmzrevamp.mixin;

import com.dmzrevamp.compat.DmzSparkingCompat;
import com.dmzrevamp.revamp.defaults.DefaultConfigSnapshots;
import com.dragonminez.common.config.DefaultFormsFactory;
import com.dragonminez.common.config.FormConfig;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;
import java.util.Map;

@Mixin(DefaultFormsFactory.class)
public abstract class DefaultFormsFactoryRevampMixin {
    @Inject(method = "createDefaultFormsForRace", at = @At("RETURN"), remap = false)
    private void dmzrevamp$normalizeRaceFormDefaults(String race, Path raceFormsDir, Map<String, FormConfig> forms, CallbackInfo ci) {
        if (DmzSparkingCompat.isLoaded() || ModList.get().isLoaded("sairens_dmz_world")) {
            return;
        }
        DefaultConfigSnapshots.applyRaceFormDefaults(race, forms);
    }

    @Inject(method = "createDefaultStackForms", at = @At("RETURN"), remap = false)
    private void dmzrevamp$normalizeStackFormDefaults(Path stackFormsDir, Map<String, FormConfig> forms, CallbackInfo ci) {
        if (DmzSparkingCompat.isLoaded()) {
            return;
        }
        DefaultConfigSnapshots.applyStackFormDefaults(forms);
    }
}
