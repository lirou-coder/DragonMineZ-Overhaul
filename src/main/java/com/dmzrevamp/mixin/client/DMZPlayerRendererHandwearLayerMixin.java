package com.dmzrevamp.mixin.client;

import com.dmzrevamp.client.HandwearDmzArmorLayer;
import com.dragonminez.client.render.DMZPlayerRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.model.GeoModel;

@Mixin(DMZPlayerRenderer.class)
public abstract class DMZPlayerRendererHandwearLayerMixin {
    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void dmzrevamp$addHandwearArmorLayer(EntityRendererProvider.Context context, GeoModel model, CallbackInfo ci) {
        DMZPlayerRenderer renderer = (DMZPlayerRenderer) (Object) this;
        renderer.addRenderLayer(new HandwearDmzArmorLayer(renderer));
    }
}
