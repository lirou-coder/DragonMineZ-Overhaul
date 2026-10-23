package com.dmzrevamp.mixin.compat.sdu;

import com.dmzrevamp.compat.SduCompat;
import com.dragonminez.client.gui.character.CharacterCustomizationScreen;
import com.dragonminez.common.stats.character.Character;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;

@Mixin(CharacterCustomizationScreen.class)
public abstract class SduCharacterClassSelectionMixin {
    @Shadow(remap = false)
    @Final
    private Character character;

    @Inject(method = "getRaceClasses", at = @At("RETURN"), cancellable = true, remap = false)
    private void dmzrevamp$filterSuppressedClassesForSelection(CallbackInfoReturnable<String[]> cir) {
        String[] classes = cir.getReturnValue();
        if (classes == null || classes.length == 0) {
            return;
        }

        String currentClass = character != null ? character.getCharacterClass() : null;
        String[] visibleClasses = Arrays.stream(classes)
                .filter(classId -> classId != null
                        && (!SduCompat.isClassSuppressed(classId) || classId.equalsIgnoreCase(currentClass)))
                .toArray(String[]::new);
        cir.setReturnValue(visibleClasses);
    }
}
