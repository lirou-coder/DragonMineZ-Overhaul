package com.dmzrevamp.mixin.compat.sdu;

import com.dmzrevamp.compat.sdu.RagnarokFormOverhaulData;
import com.google.gson.JsonObject;
import net.shurui.dev.sdu.form.FormData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(value = FormData.class, remap = false)
public abstract class SduFormDataOverhaulMixin implements RagnarokFormOverhaulData {
    @Unique private int dmzrevamp$requiredDmzLevel;
    @Override public int dmzrevamp$getRequiredDmzLevel() { return dmzrevamp$requiredDmzLevel; }
    @Override public void dmzrevamp$setRequiredDmzLevel(int value) { dmzrevamp$requiredDmzLevel = Math.max(0, value); }

    @Inject(method = "toJson", at = @At("RETURN"), require = 0)
    private void dmzrevamp$writeRequiredLevel(CallbackInfoReturnable<JsonObject> cir) {
        if (dmzrevamp$requiredDmzLevel > 0) cir.getReturnValue().addProperty("requiredDMZLevel", dmzrevamp$requiredDmzLevel);
        else cir.getReturnValue().remove("requiredDMZLevel");
    }

    @Inject(method = "fromJson(Ljava/lang/String;Lcom/google/gson/JsonObject;Ljava/lang/String;)Lnet/shurui/dev/sdu/form/FormData;", at = @At("RETURN"), require = 0)
    private static void dmzrevamp$readRequiredLevel(String key, JsonObject json, String group, CallbackInfoReturnable<FormData> cir) {
        int value = json.has("requiredDMZLevel") ? json.get("requiredDMZLevel").getAsInt() : 0;
        ((RagnarokFormOverhaulData) cir.getReturnValue()).dmzrevamp$setRequiredDmzLevel(value);
    }
}
