package com.dmzrevamp.mixin.compat.sdu;

import com.dmzrevamp.compat.sdu.RagnarokObjectiveOverhaulData;
import com.google.gson.JsonObject;
import net.shurui.dev.sdu.saga.SagaData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(value = SagaData.Objective.class, remap = false)
public abstract class SduObjectiveDataOverhaulMixin implements RagnarokObjectiveOverhaulData {
    @Unique private final JsonObject dmzrevamp$options = new JsonObject();
    @Override public JsonObject dmzrevamp$getOverhaulOptions() { return dmzrevamp$options; }

    @Inject(method = "toJson", at = @At("RETURN"), require = 0)
    private void dmzrevamp$writeOptions(CallbackInfoReturnable<JsonObject> cir) {
        dmzrevamp$options.entrySet().forEach(entry -> cir.getReturnValue().add(entry.getKey(), entry.getValue().deepCopy()));
    }

    @Inject(method = "fromJson", at = @At("RETURN"), require = 0)
    private static void dmzrevamp$readOptions(JsonObject json, CallbackInfoReturnable<SagaData.Objective> cir) {
        JsonObject options = ((RagnarokObjectiveOverhaulData) cir.getReturnValue()).dmzrevamp$getOverhaulOptions();
        for (String key : new String[]{"Armor", "ArmorToughness", "Protection", "mobEffect"}) {
            if (json.has(key)) options.add(key, json.get(key).deepCopy());
        }
    }
}
