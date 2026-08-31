package com.dmzrevamp.mixin;

import com.dmzrevamp.entity.FusionNpcArsenal;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.techniques.PredefinedTechniques;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PredefinedTechniques.class, remap = false)
public abstract class PredefinedFusionTechniquesMixin {
    @Shadow
    private void registerKi(String id, String nameKey, String user, KiAttackData.KiType type,
                            float damage, int colorMain, int colorBorder, float size,
                            float speed, int cost, String sound) {}

    @Inject(method = "init", at = @At("RETURN"))
    private void dmzrevamp$registerFusionWaves(CallbackInfo ci) {
        if (!PredefinedTechniques.REGISTRY.containsKey("final_kamehameha")) {
            registerKi("final_kamehameha", "technique.dmzrevamp.final_kamehameha", "Vegetto",
                    KiAttackData.KiType.WAVE, 2.0F, FusionNpcArsenal.FINAL_KAME_CORE,
                    FusionNpcArsenal.FINAL_KAME_OUTER, 2.0F, 1.2F, 20, "ki.kameha");
        }
        if (!PredefinedTechniques.REGISTRY.containsKey("big_bang_kamehameha")) {
            registerKi("big_bang_kamehameha", "technique.dmzrevamp.big_bang_kamehameha", "Gogeta",
                    KiAttackData.KiType.WAVE, 2.0F, FusionNpcArsenal.BIG_BANG_KAME_CORE,
                    FusionNpcArsenal.BIG_BANG_KAME_OUTER, 2.0F, 1.2F, 20, "ki.kameha");
        }
    }
}
