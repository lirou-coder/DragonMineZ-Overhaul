package com.dmzrevamp.mixin.compat.sdu;

import net.minecraft.network.chat.Component;
import net.shurui.dev.sdu.client.gui.DmzTextureButton;
import net.shurui.dev.sdu.client.gui.saga.SagaBaseScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

@Pseudo
@Mixin(value = SagaBaseScreen.class, remap = false)
public interface SduSagaBaseScreenInvoker {
    @Invoker("btn")
    DmzTextureButton dmzrevamp$button(int x, int y, int width, int height, Component text, Runnable action);

    @Invoker("footerY")
    int dmzrevamp$footerY();

    @Invoker("footerBtnHeight")
    int dmzrevamp$footerButtonHeight();
}
