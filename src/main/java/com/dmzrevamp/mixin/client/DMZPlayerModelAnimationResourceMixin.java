package com.dmzrevamp.mixin.client;

import com.dragonminez.client.model.DMZPlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DMZPlayerModel.class)
public abstract class DMZPlayerModelAnimationResourceMixin {
    @Unique
    private static final ResourceLocation DMZREVAMP_PLAYER_MOVEMENT_ANIM = ResourceLocation.fromNamespaceAndPath("dmzrevamp", "animations/entity/races/movement.animation.json");
    @Unique
    private static final ResourceLocation[] DMZREVAMP_PLAYER_ANIM_FALLBACKS = new ResourceLocation[]{
            ResourceLocation.fromNamespaceAndPath("dmzrevamp", "animations/entity/races/combat.animation.json"),
            ResourceLocation.fromNamespaceAndPath("dmzrevamp", "animations/entity/races/ki.animation.json"),
            ResourceLocation.fromNamespaceAndPath("dmzrevamp", "animations/entity/races/namekregen.animation.json"),
            ResourceLocation.fromNamespaceAndPath("dragonminez", "animations/entity/races/combat.animation.json"),
            ResourceLocation.fromNamespaceAndPath("dragonminez", "animations/entity/races/ki.animation.json"),
            ResourceLocation.fromNamespaceAndPath("dragonminez", "animations/entity/races/transf.animation.json"),
            ResourceLocation.fromNamespaceAndPath("dragonminez", "animations/entity/races/skp.animation.json")
    };

    @Inject(method = "getAnimationResource(Lnet/minecraft/client/player/AbstractClientPlayer;)Lnet/minecraft/resources/ResourceLocation;", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzrevamp$useRevampPlayerAnimationResource(AbstractClientPlayer player, CallbackInfoReturnable<ResourceLocation> cir) {
        cir.setReturnValue(DMZREVAMP_PLAYER_MOVEMENT_ANIM);
    }

    @Inject(method = "getAnimationResourceFallbacks(Lnet/minecraft/client/player/AbstractClientPlayer;)[Lnet/minecraft/resources/ResourceLocation;", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzrevamp$useRevampPlayerAnimationFallbacks(AbstractClientPlayer player, CallbackInfoReturnable<ResourceLocation[]> cir) {
        cir.setReturnValue(DMZREVAMP_PLAYER_ANIM_FALLBACKS);
    }
}
