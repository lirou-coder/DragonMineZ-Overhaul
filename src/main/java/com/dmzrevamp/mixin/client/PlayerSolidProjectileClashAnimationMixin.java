package com.dmzrevamp.mixin.client;

import com.dmzrevamp.client.ClientSolidProjectileClash;
import com.dragonminez.client.animation.AnimationCache;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.techniques.KiAttackData;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.object.PlayState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mixin(value = AbstractClientPlayer.class, priority = 900)
public abstract class PlayerSolidProjectileClashAnimationMixin {
    @Unique private static final Map<UUID, String> DMZREVAMP_LOCKED_FIRE_ANIMATIONS = new HashMap<>();

    @Inject(method = "kiPredicate", at = @At("HEAD"), cancellable = true, remap = false)
    private <T extends GeoAnimatable> void dmzrevamp$holdProjectileFireFrame(AnimationState<T> state,
                                                                              CallbackInfoReturnable<PlayState> cir) {
        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
        AbstractKiProjectile projectile = ClientSolidProjectileClash.projectileFor(player);
        if (projectile == null) {
            DMZREVAMP_LOCKED_FIRE_ANIMATIONS.remove(player.getUUID());
            return;
        }

        String animation = dmzrevamp$resolveFireAnimation(player, projectile);
        if (animation == null) return;
        String previous = DMZREVAMP_LOCKED_FIRE_ANIMATIONS.put(player.getUUID(), animation);
        if (!animation.equals(previous)) {
            state.getController().setAnimation(AnimationCache.getPlayAndHold(animation));
            state.getController().forceAnimationReset();
        }
        // Play-and-hold naturally reaches and freezes at the final firing keyframe.
        cir.setReturnValue(PlayState.CONTINUE);
    }

    @Inject(method = "predicate", at = @At("HEAD"), cancellable = true, remap = false)
    private <T extends GeoAnimatable> void dmzrevamp$blockCompetingAnimations(AnimationState<T> state,
                                                                               CallbackInfoReturnable<PlayState> cir) {
        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
        if (ClientSolidProjectileClash.projectileFor(player) != null) {
            // The dedicated Ki controller owns the bones until the solid-projectile clash ends.
            cir.setReturnValue(PlayState.STOP);
        }
    }

    @Unique
    private static String dmzrevamp$resolveFireAnimation(AbstractClientPlayer player, AbstractKiProjectile projectile) {
        String techniqueId = projectile.getTechniqueId();
        if (techniqueId == null || techniqueId.isBlank()) return null;
        return StatsProvider.get(StatsCapability.INSTANCE, player)
                .map(data -> data.getTechniques().getUnlockedTechniques().get(techniqueId))
                .filter(KiAttackData.class::isInstance)
                .map(KiAttackData.class::cast)
                .map(data -> data.getAnimationPrefix() + "_fire")
                .orElse(null);
    }
}
