package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.ki.KiParryReflector;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.dragonminez.server.events.players.combat.CombatEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces DMZ's random projectile diversion at its confirmed-parry call site. */
@Mixin(value = CombatEvent.class, remap = false)
public abstract class CombatEventKiParryReflectMixin {
    @Inject(method = "divertKiProjectile", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void dmzrevamp$reflectDirectParriedProjectile(DamageSource source, Player defender, CallbackInfo ci) {
        Entity direct = source.getDirectEntity();
        if (!(direct instanceof AbstractKiProjectile projectile)) return;
        if (defender instanceof ServerPlayer serverPlayer) {
            KiParryReflector.reflectIfSupported(projectile, serverPlayer);
        }
        // Consume the vanilla DMZ random deflection too for unsupported Ki attacks
        // (explosions, shields, barriers and other non-trajectory effects).
        ci.cancel();
    }
}
