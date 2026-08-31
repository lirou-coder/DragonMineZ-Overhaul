package com.dmzrevamp.mixin;

import com.dmzrevamp.compat.DmzSkillProgressionCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.lcd.dmzskillprogression.registry.TechniqueRegistry", remap = false)
public abstract class SkillProgressionTechniqueRegistryMixin {
    @Inject(method = "isCustomTechniqueId", at = @At("RETURN"), cancellable = true, require = 0)
    private static void dmzrevamp$excludeCustomStrikesFromKiProgression(String id, CallbackInfoReturnable<Boolean> cir) {
        if (DmzSkillProgressionCompat.isCustomStrike(id)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isCoveredSignatureAttack", at = @At("RETURN"), cancellable = true, require = 0)
    private static void dmzrevamp$includeFusionKiAttacks(String id, CallbackInfoReturnable<Boolean> cir) {
        if (DmzSkillProgressionCompat.isOverhaulSignatureKiAttack(id)) {
            cir.setReturnValue(true);
        }
    }
}
