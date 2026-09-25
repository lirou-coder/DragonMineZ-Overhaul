package com.dmzrevamp.mixin;

import com.dmzrevamp.entity.FusionNpcArsenal;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.techniques.PredefinedTechniques;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PredefinedTechniques.class, remap = false)
public abstract class PredefinedFusionTechniquesMixin {
    /**
     * DMZ 2.2 changed its private registerKi helper to include a third/outline
     * colour. Do not shadow that private implementation: these are Overhaul
     * techniques, so construct their data explicitly against the public
     * KiAttackData API and put them in the public predefined registry.
     */
    @Unique
    private static void dmzrevamp$registerFusionKi(String id, String nameKey, String user,
                                                    KiAttackData.KiType type, float damage,
                                                    int colorCore, int colorBorder, int colorOutline,
                                                    float size, float speed, int cooldownSeconds,
                                                    String animation) {
        KiAttackData data = new KiAttackData();
        data.setId(id);
        data.setName(nameKey);
        data.setAuthor(user);
        data.setKiType(type);
        data.setUtility(KiAttackData.Utility.DAMAGE);
        data.setDamageMultiplier(damage);
        data.setColorInterior(colorCore);
        data.setColorExterior(colorBorder);
        data.setColorOutline(colorOutline);
        data.setSize(size);
        data.setSpeed(speed);
        data.setArmorPenetration(0);
        data.getAllowedRaces().add("ALL");
        data.setAnimation(animation);
        data.setCastTime(5 * 20);
        data.setCooldown(cooldownSeconds);
        data.calculateDerivedValues();
        PredefinedTechniques.REGISTRY.put(id, data);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void dmzrevamp$registerFusionWaves(CallbackInfo ci) {
        if (!PredefinedTechniques.REGISTRY.containsKey("final_kamehameha")) {
            dmzrevamp$registerFusionKi(
                    "final_kamehameha",
                    "technique.dmzrevamp.final_kamehameha",
                    "Vegetto",
                    KiAttackData.KiType.WAVE,
                    2.0F,
                    FusionNpcArsenal.FINAL_KAME_CORE,
                    FusionNpcArsenal.FINAL_KAME_OUTER,
                    FusionNpcArsenal.FINAL_KAME_OUTLINE,
                    2.0F,
                    1.2F,
                    20,
                    "ki.kameha"
            );
        }
        if (!PredefinedTechniques.REGISTRY.containsKey("big_bang_kamehameha")) {
            dmzrevamp$registerFusionKi(
                    "big_bang_kamehameha",
                    "technique.dmzrevamp.big_bang_kamehameha",
                    "Gogeta",
                    KiAttackData.KiType.WAVE,
                    2.0F,
                    FusionNpcArsenal.BIG_BANG_KAME_CORE,
                    FusionNpcArsenal.BIG_BANG_KAME_OUTER,
                    FusionNpcArsenal.BIG_BANG_KAME_OUTLINE,
                    2.0F,
                    1.2F,
                    20,
                    "ki.kameha"
            );
        }
    }
}
