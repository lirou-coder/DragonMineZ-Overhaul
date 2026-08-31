package com.dmzrevamp.mixin.compat.sdu;

import net.shurui.dev.sdu.client.gui.FieldEditScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;
import java.util.function.Consumer;

@Pseudo
@Mixin(value = FieldEditScreen.class, remap = false)
public interface SduFieldEditScreenInvoker {
    @Invoker("tf")
    void dmzrevamp$textField(String label, String value, Consumer<String> setter);
}
