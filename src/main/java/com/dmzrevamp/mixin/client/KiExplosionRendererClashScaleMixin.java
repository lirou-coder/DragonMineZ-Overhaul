package com.dmzrevamp.mixin.client;

import com.dragonminez.client.init.entities.renderer.ki.KiExplosionRenderer;
import com.dragonminez.common.init.entities.ki.KiExplosionEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Lets the synchronized Explosion radius shrink below DMZ's fixed 5x render floor during a clash. */
@Mixin(KiExplosionRenderer.class)
public abstract class KiExplosionRendererClashScaleMixin {
    @Redirect(
            method = "lambda$render$0",
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F"),
            remap = false
    )
    private static float dmzrevamp$useLiveExplosionRadius(float maxRadius, float vanillaMinimum) {
        return Math.max(0.01F, maxRadius);
    }

    /**
     * DMZ centers the mesh by half of the entity hitbox height. The Overhaul
     * expands that hitbox after a win, so retain Explosion's original two-block
     * render offset instead of moving the sphere upward with its new diameter.
     */
    @Redirect(
            method = "lambda$render$0",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/init/entities/ki/KiExplosionEntity;getBbHeight()F"),
            remap = false,
            require = 0
    )
    private static float dmzrevamp$keepMeshCenteredOnOwnerDev(KiExplosionEntity explosion) {
        return Math.min(2.0F, explosion.getBbHeight());
    }

    /** Production DMZ is distributed with Mojang names remapped to SRG names. */
    @Redirect(
            method = "lambda$render$0",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/init/entities/ki/KiExplosionEntity;m_20206_()F"),
            remap = false,
            require = 0
    )
    private static float dmzrevamp$keepMeshCenteredOnOwnerSrg(KiExplosionEntity explosion) {
        return Math.min(2.0F, explosion.getBbHeight());
    }

    /** Some compilers emit the inherited method with Entity as its bytecode owner. */
    @Redirect(
            method = "lambda$render$0",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;m_20206_()F"),
            remap = false,
            require = 0
    )
    private static float dmzrevamp$keepMeshCenteredOnOwnerSrgInherited(Entity explosion) {
        return Math.min(2.0F, explosion.getBbHeight());
    }
}
