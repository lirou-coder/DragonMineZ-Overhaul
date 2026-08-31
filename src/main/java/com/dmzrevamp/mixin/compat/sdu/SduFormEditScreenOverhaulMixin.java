package com.dmzrevamp.mixin.compat.sdu;

import com.dmzrevamp.compat.sdu.RagnarokFormOverhaulData;
import net.shurui.dev.sdu.client.gui.form.FormEditScreen;
import net.shurui.dev.sdu.form.FormData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = FormEditScreen.class, remap = false)
public abstract class SduFormEditScreenOverhaulMixin {
    @Shadow private FormData form;
    @Shadow private int section;

    @Inject(method = "buildSection", at = @At("RETURN"), require = 0)
    private void dmzrevamp$addRequiredDmzLevel(CallbackInfo ci) {
        if (section != 5) return;
        RagnarokFormOverhaulData extra = (RagnarokFormOverhaulData) form;
        ((SduFieldEditScreenInvoker) this).dmzrevamp$textField("Required DMZ Level", Integer.toString(extra.dmzrevamp$getRequiredDmzLevel()), text -> {
            try { extra.dmzrevamp$setRequiredDmzLevel(Integer.parseInt(text.trim())); }
            catch (RuntimeException ignored) { }
        });
    }
}
